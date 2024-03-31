(defproject tiira-watcher "0.11.0"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"]
                 [clj-tagsoup "0.3.0" :exclusions [org.clojure/clojure]]
                 [lupapiste/geo-conversion "0.5.1" :exclusions [org.eclipse.emf/org.eclipse.emf.common org.eclipse.emf/org.eclipse.emf.ecore]]
                 [org.clojure/data.xml "0.0.8"]
                 [lurodrigo/firestore-clj "1.2.1" :exclusions [com.google.errorprone/error_prone_annotations]]
                 [camel-snake-kebab "0.4.3"]  ; For converting "this" to :this with firestore
                 [environ "1.2.0"]
                 [com.taoensso/timbre "6.2.2"]
                 [com.fzakaria/slf4j-timbre "0.4.0"]
                 [clj-time "0.15.2"]
                 [ring/ring-core "1.10.0"]
                 [ring/ring-jetty-adapter "1.10.0"]
                 [ring/ring-json "0.5.1"]
                 [ring-cors "0.1.13"]
                 [compojure "1.7.0"]
                 ]
  :source-paths ["src"]
  :repl-options {:init-ns tiira-watcher.core}
  :main tiira-watcher.core
  :plugins [[lein-environ "1.2.0"]]
  :profiles {:dev {:dependencies [
                                  [se.haleby/stub-http "0.2.14"]
                                  ]
                   :env { :firestore-credentials-file
                            "/home/tfager/pubgit/tfager/tiira-watcher/terraform/gcp-credentials.json"
                         }
                   }
              :uberjar {:aot :all}
             })
