(ns tiira-watcher.firestore
  (:require
    [firestore-clj.core :as fs]
    [camel-snake-kebab.core :as csk]
    [camel-snake-kebab.extras :as cske]
    [environ.core :refer [env]]
            ))

(def sightings-index "sightings")
(defn connect-db []
  (if (contains? env :firestore-credentials-file)
    (fs/client-with-creds (:firestore-credentials-file env))
    (if (contains? env :firestore-project-id)
      (fs/default-client (:firestore-project-id env))
      (throw (Exception.
               (str "Unable to connect to Firestore DB, neither FIRESTORE_CREDENTIALS_FILE "
               "nor FIRESTORE_PROJECT_ID env var found"))))))

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
