(ns tiira-watcher.server
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [tiira-watcher.firestore :as store]
            [tiira_watcher.tiira :as tiira]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as rjson]
            [ring.util.response :as resp]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clj-time.core :as ct]
            [clj-time.coerce :as cc]

            )
  (:gen-class)
  )

(def db (store/connect-db))

(defn start-of-day [the-date]
  (ct/date-time (ct/year the-date) (ct/month the-date) (ct/day the-date)))

(defn get-sightings [request]
  (let [days-before (get request :daysBefore 1)
        start-timestamp (start-of-day (ct/minus (ct/now) (ct/days days-before)))]
    (assoc
      (resp/response
      {:sightingGroups
       (cske/transform-keys csk/->camelCaseKeyword
         (tiira/group-by-location
           (store/read-sightings-timed db (cc/to-long start-timestamp))))})
      :headers { "Access-Control-Allow-Origin" "http://localhost:3000" }
    )))


(defroutes api-routes
           (GET "/sightings" [] get-sightings)
           (route/not-found "<h1>Page not found</h1>"))

(def app
  (rjson/wrap-json-response api-routes))

(defn -main []
  (jetty/run-jetty app {:port  8080
                        :join? false}))
