(defproject tiira-watcher "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.1"]
                 [clj-tagsoup "0.3.0" :exclusions [org.clojure/clojure]]
                 [lupapiste/geo-conversion "0.4.0" :exclusions [org.eclipse.emf/org.eclipse.emf.common org.eclipse.emf/org.eclipse.emf.ecore]]
                 [org.clojure/data.xml "0.0.8"]
                 ]
  :source-paths ["src"]
  :repl-options {:init-ns tiira_watcher.main}
  :main tiira_watcher.main
  :profiles {:dev {:dependencies [
                                  [se.haleby/stub-http "0.2.12"]
                                  ]}})
