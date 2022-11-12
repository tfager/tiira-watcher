(ns tiira-watcher.firestore
  (:require
    [firestore-clj.core :as fs]
    [camel-snake-kebab.core :as csk]
    [camel-snake-kebab.extras :as cske]
    [environ.core :refer [env]]
    [clojure.spec.alpha :as s]
    ))

(def sightings-index "sightings")
(def search-requests-index "search_requests")

(defn connect-db []
  (defonce db-conn
           (if (contains? env :firestore-credentials-file)
             (fs/client-with-creds (:firestore-credentials-file env))
             (if (contains? env :firestore-project-id)
               (fs/default-client (:firestore-project-id env))
               (throw (Exception.
                        (str "Unable to connect to Firestore DB, neither FIRESTORE_CREDENTIALS_FILE "
                             "nor FIRESTORE_PROJECT_ID env var found"))))))
  db-conn)

(defn write-sighting [db sighting]
  (-> (fs/doc (fs/coll db sightings-index) (str (:id sighting)))
      (fs/set! (cske/transform-keys csk/->kebab-case-string sighting))))

(defn read-sightings-timed [db start-timestamp]
  (map
    #(cske/transform-keys csk/->kebab-case-keyword %)
    (-> (fs/coll db sightings-index)
        (fs/filter>= "timestamp" start-timestamp)
        (fs/order-by "timestamp" :desc)
        fs/pullv)
    ))

(defn clean-sightings [db end-timestamp]
  (fs/delete-all! (-> (fs/coll db sightings-index)
                      (fs/filter<= "timestamp" end-timestamp))))

(defn write-search-request [db search-request]
  {:pre [(s/valid? :tiira/search-req-complete search-request)]}
  (-> (fs/doc (fs/coll db search-requests-index) (str (:id search-request)))
      (fs/set! (cske/transform-keys csk/->kebab-case-string search-request))))
