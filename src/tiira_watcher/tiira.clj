(ns tiira_watcher.tiira
  (:require [clj-http.client :as http]
            [clj-http.cookies :as cookies]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [pl.danieljanus.tagsoup :as tags]
            [geo-conversion.core :as geo]
            [clj-time.format :as timefmt]
            [clj-time.coerce :as timec]
            )
  (:import (java.nio.charset StandardCharsets)))
;            [taoensso.timbre :as timbre :refer [log trace debug info warn]


(def ^:dynamic *tiira-base-uri* "https://www.tiira.fi")
(def username-field "tunnus")
(def password-field "salasana")
(def login-constants {"kirjaudutaan" "2" "login" "KIRJAUDU"})
(def cookie-store (clj-http.cookies/cookie-store))
(def no-sightings-string "<small>Ei havaintoja</small>")
(def next-page-string ">Seuraava")
(def spotter-re #"Havainnoijan paikka: (\d+):(\d+)")
(def bird-loc-re #"Linnun paikka: (\d+):(\d+)")
(def time-re #"Havainnointiaika: ([^<]+)")
(def min-coordinate-difference 1.0)
(def findate-formatter (timefmt/formatter-local "dd.MM.yyyy"))

(def tiira-http-defaults {
                          :cookie-store cookie-store        ; When cookie-store is there, session is stored automatically
                          :as           :auto               ; detect charset
                          })
(def adv-search-defaults {
                          "toiminto"     "29"
                          "laji"         ""
                          "alue"         "0"                ; 0 = Entire Finland
                          "kunta"        ""
                          "kerutoim"     "1"
                          "order1"       "pvm1"
                          "suunta1"      "desc"
                          "order2"       "syst_ord"
                          "suunta2"      "asc"
                          "limit"        "50"
                          "tilat"        "kaikki"
                          "yksmaara"     "1"
                          "paivamaara"   "1"
                          "paivamaara_a" ""
                          "paivamaara_l" ""
                          "aikaalue1"    ""
                          "aikaalue2"    ""
                          "haku"         "Hae"
                          })

(defn tiira-login [username password]
  (let [resp (http/post (str *tiira-base-uri* "/index.php?toiminto")
                        (merge tiira-http-defaults
                               {:form-params (assoc login-constants username-field username password-field password)
                                }))]
    (not (str/includes? (:body resp) "alert('Kirjautuminen epäonnistui!"))
    ))

(defn sighting-table? [tag] (and (= :table (first tag))
                                 (= "havaintolistaus" (:class (second tag) ""))))

(defn session-exists? []
  (contains? (cookies/get-cookies cookie-store) "PHPSESSID"))

(defn find-rec [parsed-data pred-fn result-so-far]
  (if (pred-fn parsed-data)
    (concat result-so-far [parsed-data])
    (reduce (fn [result child]
              (if (vector? child)
                (let [enhanced-result (find-rec child pred-fn result)]
                  enhanced-result)
                result))
            result-so-far
            (subvec parsed-data 2))))

(defn trim [s]
  (if (nil? s) s (str/trim s)))

(defn -parse-search-result [s-tags]
  {
   :species  (-> s-tags (get-in [5 2 2]) (trim))
   :date     (-> s-tags (get-in [6 2]) (trim))
   :county   (-> s-tags (get-in [7 2]) (trim))
   :id       (-> s-tags (get-in [8 2 1 :href]) (str/replace #"[^\d]" ""))
   :loc-name (-> s-tags (get-in [8 2 1 :title]) (str/replace #" Näytä havaintopaikka kartalla" ""))
   :count    (-> s-tags (get-in [9 2]) (trim))
   :extra    (-> s-tags (get-in [10 2]) (trim))
   }
  )

(defn -add-timestamp [sighting]
  (assoc sighting
    :timestamp
    (try
      (timec/to-long (timefmt/parse findate-formatter (:date sighting)))
      (catch Exception e (System/currentTimeMillis)))))

(defn -parse-search-results [html]
  (let [parsed (-> html
                   ; Tagsoup would find the latin-1 encoding in HTML.
                   ; But it's a lie, HTTP-client has decoded it already, so replace with truth.
                   (str/replace "charset=iso-8859-1" "charset=utf-8")
                   (tags/parse-string))
        stable (first (find-rec parsed sighting-table? []))
        sightings (drop 3 stable)
        result (map -parse-search-result sightings)
        result (map -add-timestamp result)
        ]
    result
    ))


(defn find-next-page-num [content]
  (second (re-find #"haku=Hae&amp;sivu=(\d+)\" onmouseover=\"javascript: ohje\('Seuraava'" content)))

(defn advanced-search
  ([] (advanced-search 0 []))
  ([page-num results-so-far]
   (let [query-params (if (= 0 page-num) adv-search-defaults (assoc adv-search-defaults :sivu (str page-num)))
         resp (http/get (str *tiira-base-uri* "/index.php")
                        (merge tiira-http-defaults
                               {:query-params query-params}))]
     (if (str/includes? (:body resp) no-sightings-string)
       []
       (if (str/includes? (:body resp) next-page-string)
         (let [next-page-num (find-next-page-num (:body resp))]
           ;(println "Getting page " next-page-num
           ;         ", " (count results-so-far) " results so far")
           (advanced-search next-page-num
                            (concat results-so-far (-parse-search-results (:body resp))))
           )
         ;; Else - last page
         (concat results-so-far (-parse-search-results (:body resp)))
         )
       ))))

(s/def :tiira/minx float?)
(s/def :tiira/miny float?)
(s/def :tiira/maxx float?)
(s/def :tiira/maxy float?)
(s/def :tiira/coords (s/keys :req-un [:tiira/minx :tiira/miny :tiira/maxx :tiira/maxy]))

(defn store-map-border [borders]
  {:pre [(s/valid? :tiira/coords borders)]}

  (let [resp (http/post (str *tiira-base-uri* "/kartta/koord_rajaus/index.php")
                        (merge tiira-http-defaults
                               {:form-params         (assoc borders
                                                       "imagewidth" "500"
                                                       "imageheight" "500"
                                                       "cmd" "coordsele"
                                                       "input_coord" "0,499;499,0" ; Select entire area
                                                       "distanceParam" ""
                                                       "distanceMeters" ""
                                                       "zoomfactor" "4"
                                                       "koord_valmis" "Hyväksy")
                                :form-param-encoding "ISO-8859-1"
                                }))]
    (str/includes? (:body resp) "Valittu alue")             ; This text is there in OK response
    ))

(defn convert-coordinates [sighting]
  (let [lat (:bird-latitude sighting (:spotter-latitude sighting))
        long (:bird-longitude sighting (:spotter-longitude sighting))
        [wgslong wgslat] (geo/epsg3067->wgs84 [long lat])]
    (assoc sighting :wgs-latitude wgslat
                    :wgs-longitude wgslong
                    :osm-url (str "https://www.openstreetmap.org/#map=17/" wgslat "/" wgslong))))

(s/def :tiira/sighting-id string?)
(s/def :tiira/raw-sighting (s/keys :req-un [:tiira/sighting-id]))

(defn enrich-sighting [raw-sighting]
  "Enrich sighting with coordinates"
  {:pre [(s/valid? :tiira/raw-sighting raw-sighting)]}

  (let [resp (http/get (str *tiira-base-uri* "/selain/naytahavis.php")
                       (merge tiira-http-defaults
                              {:query-params {:id (:id raw-sighting)}}))
        body (:body resp)
        ]
    (convert-coordinates
      (merge raw-sighting
             (when-let [[_ spotter-lat spotter-long] (re-find spotter-re body)]
               {:spotter-latitude (float (read-string spotter-lat)) :spotter-longitude (float (read-string spotter-long))})
             (when-let [[_ bird-lat bird-long] (re-find bird-loc-re body)]
               {:bird-latitude (float (read-string bird-lat)) :bird-longitude (float (read-string bird-long))})
             (when-let [[_ sighting-time] (re-find time-re body)]
               {:time sighting-time})))
    ))
(s/def :tiira/spotter-latitude float?)
(s/def :tiira/spotter-longitude float?)
(s/def :tiira/loc-sighting (s/keys :req-un [:tiira/spotter-latitude :tiira/spotter-longitude]))
(s/def :tiira/sightings (s/coll-of :tiira/loc-sighting))
(s/def :tiira/loc-name string?)
(s/def :tiira/sighting-bucket (s/keys :req-un [:tiira/loc-name :tiira/sightings]))
(defn group-by-location [sightings]
  {:pre  [(s/valid? :tiira/sightings sightings)]
   :post [(s/valid? (s/coll-of :tiira/sighting-bucket) %)]}
  (let [groups (group-by (juxt :spotter-latitude :spotter-longitude) sightings)]
    (map (fn [group] {:loc-name          (:loc-name (first group))
                      :spotter-latitude  (:spotter-latitude (first group))
                      :spotter-longitude (:spotter-longitude (first group))
                      :bird-latitude     (:bird-latitude (first group))
                      :bird-longitude    (:bird-longitude (first group))
                      :wgs-latitude      (:wgs-latitude (first group))
                      :wgs-longitude     (:wgs-longitude (first group))
                      :sightings         group})
         (vals groups))
    ))
