(ns tiira-watcher.server
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [tiira-watcher.firestore :as store]
            [tiira-watcher.tiira :as tiira]
            [tiira-watcher.logic :as logic]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as rjson]
            [ring.middleware.params :as rparams]
            [ring.middleware.keyword-params :as kparams]
            [ring.util.response :as resp]
            [ring.middleware.cors :as rcors]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clj-time.core :as ct]
            [clj-time.coerce :as cc]
            [environ.core :refer [env]]
            [clojure.spec.alpha :as s]
            [taoensso.timbre :refer [info]]
            )
  (:gen-class)
  )
(def ui-server-address-regex (re-pattern (:ui-server-address env "http://localhost:3000")))

(defn start-of-day [the-date]
  (ct/date-time (ct/year the-date) (ct/month the-date) (ct/day the-date)))

(defn get-sightings [request]
  (let [days-before (read-string (get-in request [:params :daysBefore] "1"))
        start-timestamp (start-of-day (ct/minus (ct/now) (ct/days days-before)))
        _ (info "Get-sightings: days-before = " days-before ", start-timestamp = " start-timestamp)
        db (store/connect-db)]
    (resp/response
      {:sightingGroups
       (cske/transform-keys csk/->camelCaseKeyword
         (tiira/group-by-location
           (store/read-sightings-timed db (cc/to-long start-timestamp))))})
    ))

(s/def :tiira/area string?)
(s/def :tiira/username string?)
(s/def :tiira/id string?)
(s/def :tiira/timestamp number?)
(s/def :tiira/search-status (set (vals logic/search-status)))
(s/def :tiira/search-req (s/keys :req-un [:tiira/area]))
(s/def :tiira/search-req-complete (s/keys :req-un [:tiira/timestamp :tiira/area :tiira/username :tiira/search-status]))

(defn enrich-search-request [search-request]
  {:pre [(s/valid? :tiira/search-req search-request)]
   :post [(s/valid? :tiira/search-req-complete %)]}
  (assoc search-request
    :id (str (random-uuid))
    :timestamp (cc/to-long (ct/now))
    :username  "TODO"
    :search-status (:new logic/search-status)
    )
  )
(defn search-sightings [request]
  (if-not (= "application/json" (get-in request [:headers "content-type"]))
    (resp/bad-request "Only encoding application/json accepted")
    (if-not (s/valid? :tiira/search-req (:body request))
      (resp/bad-request (str "Invalid body: " (s/explain-str :tiira/search-req (:body request))))
      (let [search-request (:body request)
            search-request (enrich-search-request search-request)
            db   (store/connect-db)]
        (info "Storing search request: " search-request)
        (store/write-search-request db search-request)
        (resp/response { :status :finished :id (:id search-request)})
        ))))

(defn get-search-requests [request]
  (info "Getting search requests")
  (let [db (store/connect-db)
        result (store/read-search-requests db)]
    (resp/response {:results (map #(cske/transform-keys csk/->camelCaseKeyword %) result) })))

(defroutes api-routes
           (GET "/sightings" [] get-sightings)
           (POST "/search" [] search-sightings)
           (GET "/search" [] get-search-requests)
           (route/not-found "<h1>Page not found</h1>"))

;; TODO: header X-Apigateway-Api-Userinfo has Base64 encoded JWT payload
;; Ref. https://cloud.google.com/api-gateway/docs/authenticating-users-firebase
(def app
  (-> api-routes
      (rcors/wrap-cors :access-control-allow-origin [ui-server-address-regex]
                       :access-control-allow-methods [:get :post])
      (kparams/wrap-keyword-params)
      (rparams/wrap-params)
      (rjson/wrap-json-response)
      (rjson/wrap-json-body { :keywords? true })
      ))

(defn -main []
  ; Cold-start DB to avoid race conditions later
  (store/connect-db)
  (jetty/run-jetty app {:port  8080
                        :join? false}))
