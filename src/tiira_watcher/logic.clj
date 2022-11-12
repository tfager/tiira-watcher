(ns tiira-watcher.logic
  (:require  [tiira-watcher.tiira :as tiira]
             [tiira-watcher.firestore :as store]
             [environ.core :refer [env]]
             [taoensso.timbre :refer [info]]
             [clj-time.coerce :as cc]
             ))

(def areas {
            :tikkurila   {:miny 6684066.0, :minx 389806.0
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
            })

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
                 })

(defn tiira-search [username password area]
  (tiira/tiira-login username password)
  (tiira/store-map-border (get areas (keyword area)))
  (let [result (tiira/advanced-search)
        ;_ (println "Results: " (count result))
        filtered (filter (fn [sighting] (not (contains? blacklist (:species sighting)))) result)
        ;_ (println "Filtered results: " (count filtered))
        enriched (for [s filtered]
                   (do
                     (let [es (tiira/enrich-sighting s)]
                       ; (println es)
                       (Thread/sleep 500)
                       es)))]
    enriched
    ))

(defn tiira-search-and-store [db area]
  (info "Searching " area)
  (when-not (contains? env :tiira-username) (throw (IllegalStateException. "Missing environment variable TIIRA_USERNAME")))
  (when-not (contains? env :tiira-password) (throw (IllegalStateException. "Missing environment variable TIIRA_PASSWORD")))
  (let [username (:tiira-username env)
        password (:tiira-password env)
        enriched (tiira-search username password area)]
    (doseq [s enriched]
      (info (:species s) " " (:date s) " " (:time s) " "
               (:osm-url s) " " (:loc-name s) " " (:extra s))
      (store/write-sighting db s))
    (info "Stored " (count enriched) " sightings")
    ))

(defn tiira-process-search-requests [db]
  (doseq [s-req (store/read-search-requests db)]
    (info "Processing search request " (:id s-req)
          ", timestamp " (cc/from-long (:timestamp s-req))
          ", user " (:username s-req)
          ", area " (:area s-req))
    ;; Delete first to avoid infinite loop if something goes wrong
    (store/delete-search-request db (:id s-req))
    (tiira-search-and-store db (:area s-req))))
