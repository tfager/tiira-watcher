(ns tiira_watcher.main
  (:require [tiira_watcher.tiira :as tiira]
            [tiira-watcher.firestore :as store]
            [geo-conversion.core :as geo]
            [environ.core :refer [env]]
            [java-time :as jt]
            [java-time.convert :as jtc]
            [java-time.temporal :as jtt]
            )
  (:import (java.util Date))
  )


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



(defn render-html [area sightings]
  (let [title (str "Birds in " area " " (Date.))]
    (println "<html><head><title>" title "</title></head>")
    (println "<body><ul>")
    (doseq [s sightings]
      (println "<li>" (:species s) " " (:date s) " " (:time s) " "
               "<a href=\"" (:osm-url s) "\">" (:loc-name s) "</a> (" (:extra s)))
    (println "</ul></body></html>")
    ))

(defn render-leaflet-grouped [area groups]
  (let [area-coords (get areas (keyword area))
        xfin (/ (+ (:minx area-coords) (:maxx area-coords)) 2)
        yfin (/ (+ (:miny area-coords) (:maxy area-coords)) 2)
        [y x] (geo/epsg3067->wgs84 [xfin yfin])   ; TODO: gotta fix the coordinate mixup :(
        ]
    (println (str "birdmap.setView([" x ", " y "], 13);"))
    (doseq [g groups]
      (println (str "L.marker([" (:wgs-latitude g) ", " (:wgs-longitude g) "]).addTo(birdmap)"))
      (println (str "    .bindPopup('"
                    (reduce str (str (:loc-name g) "<br>")
                            (for [s (:sightings g)]
                              (str (:species s) " " (:extra s) " " (:time s) "<br>")))
                    "');"))
      )))

(defn render-leaflet [area sightings]
  (let [area-coords (get areas (keyword area))
        xfin (/ (+ (:minx area-coords) (:maxx area-coords)) 2)
        yfin (/ (+ (:miny area-coords) (:maxy area-coords)) 2)
        [y x] (geo/epsg3067->wgs84 [xfin yfin])   ; TODO: gotta fix the coordinate mixup :(
        ]
    (println (str "birdmap.setView([" x ", " y "], 13);"))
    (doseq [s sightings]
      (println (str "L.marker([" (:wgs-latitude s) ", " (:wgs-longitude s) "]).addTo(birdmap)"))
      (println (str "    .bindPopup('" (:loc-name s) " " (:species s) " "
                    (:extra s) " " (:date s) " " (:time s) "');")))))

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
  (println "Searching " area)
  (when-not (contains? env :tiira-username) (throw (IllegalStateException. "Missing environment variable TIIRA_USERNAME")))
  (when-not (contains? env :tiira-password) (throw (IllegalStateException. "Missing environment variable TIIRA_PASSWORD")))
  (let [username (:tiira-username env)
        password (:tiira-password env)
        enriched (tiira-search username password area)]
    (doseq [s enriched]
      (println (:species s) " " (:date s) " " (:time s) " "
               (:osm-url s) " " (:loc-name s) " " (:extra s))
      (store/write-sighting db s))
    (println "Stored " (count enriched) " sightings")
    ))

(defn -main [command & rest]
  (let [clean-upto (jt/minus (jt/instant) (jt/days 7))
        db (store/connect-db)]
  (case command
    "search"    (tiira-search-and-store db (first rest))
    "clean"     (store/clean-sightings db (inst-ms clean-upto))
    )))
