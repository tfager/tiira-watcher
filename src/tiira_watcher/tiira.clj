(ns tiira_watcher.tiira
  (:require [clj-http.client :as http]
            [clj-http.cookies :as cookies]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [pl.danieljanus.tagsoup :as tags]
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

(def tiira-http-defaults {
                          :cookie-store cookie-store        ; When cookie-store is there, session is stored automatically
                          :as           :auto               ; detect charset
                          })
(def adv-search-defaults {
                          "toiminto"     "29"
                          "laji"         ""
                          "alue"         "0"   ; 0 = Entire Finland
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

(defn -parse-search-result [s-tags]
  ;(println "S-tags = " s-tags)
  {
   :species  (-> s-tags (get-in [5 2 2]) (str/trim))
   :date     (-> s-tags (get-in [6 2]) (str/trim))
   :county   (-> s-tags (get-in [7 2]) (str/trim))
   :id       (-> s-tags (get-in [8 2 1 :href]) (str/replace #"[^\d]" ""))
   :loc-name (-> s-tags (get-in [8 2 1 :title]) (str/replace #" Näytä havaintopaikka kartalla" ""))
   :count    (-> s-tags (get-in [9 2]) (str/trim))
   :extra    (-> s-tags (get-in [10 2]) (str/trim))
   }
  ;; TODO: nullpointer at row 78?
  )

(defn -parse-search-results [html]
  (let [parsed (tags/parse-string html)
        stable (first (find-rec parsed sighting-table? []))
        sightings (drop 3 stable)
        result (map -parse-search-result sightings)]
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
           (println "Getting page " next-page-num
                    ", " (count results-so-far) " results so far")
           (advanced-search next-page-num
                            (concat results-so-far (-parse-search-results (:body resp))))
         )
         ;; Else - last page
         (concat results-so-far (-parse-search-results (:body resp)))
       )
     ))))


;; TODO: UTM (or ETRS-TM35FIN) to WGS84 coordinate conversion
;; https://dev.solita.fi/2017/12/12/gis-coordinate-systems.html
;; https://www.evanlouie.com/gists/clojure/src/com/evanlouie/geocoding/utm.cljc

(s/def :tiira/minx float?)
(s/def :tiira/miny float?)
(s/def :tiira/maxx float?)
(s/def :tiira/maxy float?)
(s/def :tiira/coords (s/keys :req-un [:tiira/minx :tiira/miny :tiira/maxx :tiira/maxy]))

(def laajasalo-coords {:miny 6670099.0, :minx 388479.0 :maxy 6673264.0, :maxx 394564.0})

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
    ; value="Hyväksy" name="koord_valmis"
    ; <form name="koordform" id="koordform" method="post" action="index.php"
    ))
