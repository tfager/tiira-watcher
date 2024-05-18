(ns tiira-watcher.core
  (:require [tiira-watcher.firestore :as store]
            [tiira-watcher.server :as server]
            [tiira-watcher.logic :as logic]
            [geo-conversion.core :as geo]
            [taoensso.timbre :as log]
            )
  (:import (java.util Date))
  (:gen-class))

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
  (let [area-coords (get logic/areas (keyword area))
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
  (let [area-coords (get logic/areas (keyword area))
        xfin (/ (+ (:minx area-coords) (:maxx area-coords)) 2)
        yfin (/ (+ (:miny area-coords) (:maxy area-coords)) 2)
        [y x] (geo/epsg3067->wgs84 [xfin yfin])   ; TODO: gotta fix the coordinate mixup :(
        ]
    (println (str "birdmap.setView([" x ", " y "], 13);"))
    (doseq [s sightings]
      (println (str "L.marker([" (:wgs-latitude s) ", " (:wgs-longitude s) "]).addTo(birdmap)"))
      (println (str "    .bindPopup('" (:loc-name s) " " (:species s) " "
                    (:extra s) " " (:date s) " " (:time s) "');")))))

(defn -main [command & rest]
  (log/set-min-level! :info)
  (let [db (store/connect-db)]
  (case command
    "search"       (logic/tiira-search-and-store db (first rest))
    "clean"        (logic/clean-old-items db)
    "search-reqs"  (logic/tiira-process-search-requests db)
    "server"       (server/-main)
    )))
