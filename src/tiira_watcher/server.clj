(ns tiira-watcher.server
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [tiira-watcher.firestore :as store]
            [tiira-watcher.tiira :as tiira]
            [tiira-watcher.logic :as logic]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as rjson]
            [ring.util.response :as resp]
            [ring.middleware.cors :as rcors]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clj-time.core :as ct]
            [clj-time.coerce :as cc]
            [environ.core :refer [env]]
            [clojure.spec.alpha :as s]
            )
  (:gen-class)
  )
(def ui-server-address-regex (re-pattern (:ui-server-address env "http://localhost:3000")))

(defn start-of-day [the-date]
  (ct/date-time (ct/year the-date) (ct/month the-date) (ct/day the-date)))

(defn get-sightings [request]
  (let [days-before (get request :daysBefore 1)
        start-timestamp (start-of-day (ct/minus (ct/now) (ct/days days-before)))
        db (store/connect-db)]
    (resp/response
      {:sightingGroups
       (cske/transform-keys csk/->camelCaseKeyword
         (tiira/group-by-location
           (store/read-sightings-timed db (cc/to-long start-timestamp))))})
    ))

(s/def :tiira/area string?)
(s/def :tiira/search-req (s/keys :req-un [:tiira/area]))


(defn search-sightings [request]
  (if-not (= "application/json" (get-in request [:headers "content-type"]))
    (resp/bad-request "Only encoding application/json accepted")
    (if-not (s/valid? :tiira/search-req (:body request))
      (resp/bad-request (str "Invalid body: " (s/explain-str :tiira/search-req (:body request))))
      (let [area (get-in request [:body :area])
            db   (store/connect-db)
            ]
        (println "Starting search: " area)
        (logic/tiira-search-and-store db area)
        (resp/response { :status :finished })
        ))))


(defroutes api-routes
           (GET "/sightings" [] get-sightings)
           (POST "/search" [] search-sightings)
           (route/not-found "<h1>Page not found</h1>"))

(def app
  (-> api-routes
      (rcors/wrap-cors :access-control-allow-origin [ui-server-address-regex]
                       :access-control-allow-methods [:get])
      (rjson/wrap-json-response)
      (rjson/wrap-json-body { :keywords? true })
      ))

(defn -main []
  ; Cold-start DB to avoid race conditions later
  (store/connect-db)
  (jetty/run-jetty app {:port  8080
                        :join? false}))
