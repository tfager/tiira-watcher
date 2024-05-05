(ns tiira-watcher.server-test
    (:require [clj-time.coerce :as cc]
              [clj-time.core :as ct]
              [clojure.test :refer :all]
              [tiira-watcher.logic :as logic]
              [tiira-watcher.server :as server]))

(def one-second-millis 1000)

(deftest enrich-search-request-test
  (testing "Basic enrichment"
    (let [result (server/enrich-search-request {:area "pks"})]
         (is (< (abs (- (:timestamp result) (cc/to-long (ct/now)))) one-second-millis))
         (is (= (:area result) "pks"))
         (is (= (:search-status result) (:new logic/search-status)))
    )))



