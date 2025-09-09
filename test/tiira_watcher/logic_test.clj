
(ns tiira-watcher.logic-test
  (:require [clojure.test :refer [deftest is testing]]
            [tiira-watcher.logic :refer [calculate-bounding-box]]))

(deftest test-calculate-bounding-box
  (testing "Bounding box calculation returns map with keys"
    (let [center-lat 60.292738
          center-lon 25.044633
          diag-half-km 1
          bbox (calculate-bounding-box center-lat center-lon diag-half-km)]
      (is (map? bbox))
      (is (contains? bbox :minx))
      (is (contains? bbox :miny))
      (is (contains? bbox :maxx))
      (is (contains? bbox :maxy))))

  (testing "Bounding box values are correct"
    (let [bbox (calculate-bounding-box 60.292738 25.044633 1)]
      (is (= bbox {:minx 391180.31339, :miny 6684929.57342, :maxx 392639.06229, :maxy 6686302.02363})))))
