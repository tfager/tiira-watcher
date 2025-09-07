(ns tiira-watcher.logic
  (:require  [tiira-watcher.tiira :as tiira]
             [tiira-watcher.firestore :as store]
             [environ.core :refer [env]]
             [taoensso.timbre :refer [info]]
             [clj-time.coerce :as cc]
             [clj-time.core :as ct]
             [geo-conversion.core :as geo]

             [clojure.spec.alpha :as s]))

(def areas {:tikkurila   {:miny 6684066.0, :minx 389806.0
                          :maxy 6690151.0, :maxx 394768.0}
            :vuosaari    {:miny 6673938.0, :minx 394238.0
                          :maxy 6683821.0, :maxx 400527.0}
            :espoo       {:miny 6664382.0, :minx 368509.0
                          :maxy 6677450.0, :maxx 380434.0}
            :kirkkonummi {:miny 6649843.0, :minx 342862.0
                          :maxy 6673203.0, :maxx 368100.0}
            :laajasalo   {:miny 6670099.0, :minx 388479.0
                          :maxy 6673264.0, :maxx 394564.0}
            :viikki      {:miny 6675653.0, :minx 387479.0
                          :maxy 6678982.0, :maxx 391685.0}
            :itauusimaa  {:miny 6676634.0, :minx 397913.0
                          :maxy 6712327.0, :maxx 412779.0}
            :pks         {:miny 6669446.0, :minx 377330.0
                          :maxy 6693623.0, :maxx 404202.0}
            :pori        {:miny 6805114.0, :minx 194371.0
                          :maxy 6853794.0, :maxx 241744.0}
            :uto         {:miny 6634978.0, :minx 178117.0
                          :maxy 6647393.0, :maxx 191512.0}
            :virolahti   {:miny 6676389.0, :minx 424050.0
                          :maxy 6729970.0, :maxx 555389.0}})

(def blacklist #{"Harmaahaikara"
                 "Haarapääsky"
                 "Valkoposkihanhi"
                 "Kanadanhanhi"
                 "Kalalokki"
                 "Naurulokki"
                 "Talitiainen"
                 "Sinitiainen"
                 "Peippo"
                 "Keltasirkku"
                 "Kyhmyjoutsen"
                 "Laulujoutsen"
                 "Mustarastas"
                 "Räkättirastas"
                 "Töyhtöhyyppä"
                 "Isokoskelo"
                 "Naakka"
                 "Varis"
                 "Kirjosieppo"
                 "Joutsenlaji"
                 "Hanhilaji"
                 "Sinisorsa"
                 "Telkkä"
                 "Varpunen"
                 "Pikkuvarpunen"
                 "Tiltaltti"
                 "Västäräkki"
                 "Harakka"
                 "Sepelkyyhky"
                 "Punarinta"
                 "Kottarainen"
                 "Tikli"
                 "Nokikana"
                 "Pajusirkku"
                 "Pajulintu"
                 "Hemppo"
                 "Käpytikka"
                 "Tukkasotka"
                 "Meriharakka"
                 "Viherpeippo"})

(def search-status {:new "NEW" :searching "SEARCHING" :done "DONE"})

(defn calculate-bounding-box
  "Calculate bounding box in EPSG:3067 from center-lat, center-lon, diag-half-km."
  [center-lat center-lon diag-half-km]
  (let [diag-half-m (* diag-half-km 1000)
        half-side-m (/ diag-half-m (Math/sqrt 2))
        lat-deg-per-m (/ 1 111320.0)
        min-lat (double (- center-lat (* half-side-m lat-deg-per-m)))
        max-lat (double (+ center-lat (* half-side-m lat-deg-per-m)))
        lon-deg-per-m (/ 1 (* 111320.0 (Math/cos (Math/toRadians center-lat))))
        min-lon (double (- center-lon (* half-side-m lon-deg-per-m)))
        max-lon (double (+ center-lon (* half-side-m lon-deg-per-m)))
        [minx miny] (geo/convert-crs [min-lon min-lat] "EPSG:4326" "EPSG:3067")
        [maxx maxy] (geo/convert-crs [max-lon max-lat] "EPSG:4326" "EPSG:3067")]
    {:minx minx :miny miny :maxx maxx :maxy maxy}))

(defn tiira-search
  "Searches Tiira either by named area or by custom bounding box defined by center-lat, center-lon, diag-half-km."
  [username password {:keys [area center-lat center-lon diag-half-km] :as params}]
  (tiira/tiira-login username password)
  (if (and (not area)
           center-lat center-lon diag-half-km)
    (let [bbox (calculate-bounding-box center-lat center-lon diag-half-km)]
      (tiira/store-map-border bbox))
    (tiira/store-map-border (get areas (keyword area))))
  (let [result (tiira/advanced-search)
        filtered (filter (fn [sighting] (not (contains? blacklist (:species sighting)))) result)
        enriched (for [s filtered]
                   (let [es (tiira/enrich-sighting s)] (Thread/sleep 500) es))]
    enriched))

(defn tiira-search-and-store [db s-req]
  {:pre [(s/valid? :tiira/search-req-complete s-req)]}
  (let [{:keys [area center-lat center-lon diag-half-km]} s-req
        area-name (if (string? area) area (str "custom bbox: " center-lat " " center-lon " diag-half-km " diag-half-km))]
    (info "Searching " area-name)
    (when-not (contains? env :tiira-username) (throw (IllegalStateException. "Missing environment variable TIIRA_USERNAME")))
    (when-not (contains? env :tiira-password) (throw (IllegalStateException. "Missing environment variable TIIRA_PASSWORD")))
    (let [username (:tiira-username env)
          password (:tiira-password env)
          enriched (tiira-search username password {:area area
                                                    :center-lat center-lat
                                                    :center-lon center-lon
                                                    :diag-half-km diag-half-km})]
      (doseq [s enriched]
        (info (:species s) " " (:date s) " " (:time s) " "
              (:osm-url s) " " (:loc-name s) " " (:extra s))
        (store/write-sighting db s))
      (info "Stored " (count enriched) " sightings"))))

(defn tiira-process-search-requests [db]
  (doseq [s-req (store/read-search-requests db)]
    ; Grab only NEW requests
    (when (= (:search-status s-req) (:new search-status))
      (info "Processing search request " (:id s-req)
            ", timestamp " (cc/from-long (:timestamp s-req))
            ", user " (:username s-req)
            ", area " (:area s-req)
            ", center-lat " (:center-lat s-req)
            ", center-lon " (:center-lon s-req)
            ", diag-half-km " (:diag-half-km s-req)
            ", status " (:search-status s-req))
      (when-not (s/valid? :tiira/search-req s-req)
        (throw (IllegalStateException. (str "Invalid search request: " (:id s-req) ": " (s/explain-str :tiira/search-req s-req)))))
      ;; Update status first to avoid infinite loop if something goes wrong
      (store/update-search-status db (:id s-req) (:searching search-status))
      (tiira-search-and-store db s-req)
      (store/update-search-status db (:id s-req) (:done search-status)))))

  (defn clean-old-items [db]
    (let [sightings-date-limit (ct/minus (ct/now) (ct/days 7))
          reqs-date-limit (ct/minus (ct/now) (ct/days 2))]
      (info "Cleaning sightings older than " sightings-date-limit)
      (store/clean-sightings db (inst-ms sightings-date-limit))
      (info "Cleaning search requests older than " reqs-date-limit)
      (store/clean-search-requests db (inst-ms reqs-date-limit))))
