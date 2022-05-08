(ns tiira_watcher.main
  (:require [tiira_watcher.tiira :as tiira]
            ))

(def tikkurila-area {:miny 6684066.0, :minx 389806.0
                     :maxy 6690151.0, :maxx 394768.0})
(defn -main [& args]
  (tiira/tiira-login (first args) (second args))
  (tiira/store-map-border tikkurila-area)
  (let [result (tiira/advanced-search)]
    (doseq [r result] (println r))
    ))


