(ns tiira-watcher.tiira-test
  (:require [clojure.test :refer :all]
            [stub-http.core :as stub]
            [tiira_watcher.tiira :as tiira]
            [clojure.java.io :as io]
            [clojure.string :as str]
            ))

(def test-username "testuser")
(def test-password "testpw")
(def wrong-password "wrong")
(def test-session-id "session")
(def test-borders {:minx 255139.0
                   :miny 6590462.0
                   :maxx 418495.0
                   :maxy 6753819.0})
(def default-response {:status       200
                       :content-type "text/html"})
(def sighting-id "25860498")

(defn read-html-res [filename]
  (-> filename (io/resource) (io/file) slurp (str/replace "charset=iso-8859-1" "charset=utf-8")))

(deftest tiira-login-test
  (testing "Successful login"
    (stub/with-routes! {{:method      :post
                         :path        "/index.php"
                         :form-params (assoc tiira/login-constants
                                        tiira/username-field test-username
                                        tiira/password-field test-password)}
                        (merge default-response {
                                                 :body    "Oh yeah."
                                                 :headers {"Set-Cookie" (str "PHPSESSID=" test-session-id)}
                                                 })}
                       (binding [tiira/*tiira-base-uri* uri]
                         (let [result-ok? (tiira/tiira-login test-username test-password)]
                           (is result-ok?)
                           (is (tiira/session-exists?)))
                         )))
  (testing "Failed login"
    (stub/with-routes! {{:method      :post
                         :path        "/index.php"
                         :form-params (assoc tiira/login-constants
                                        tiira/username-field test-username
                                        tiira/password-field wrong-password)}
                        (merge default-response {
                                                 :body (-> "login_failed.html" io/resource io/file slurp)
                                                 })}
                       (binding [tiira/*tiira-base-uri* uri]
                         (let [result-ok? (tiira/tiira-login test-username wrong-password)]
                           (is (not result-ok?)))
                         )))
  )

(deftest store-map-border-test
  (stub/with-routes! {{:method      :post
                       :path        "/kartta/koord_rajaus/index.php"
                       :form-params (assoc test-borders
                                      "cmd" "coordsele"
                                      "koord_valmis" "Hyväksy")}
                      (merge default-response {
                                               :body "Valittu alue ..."
                                               })}
                     (binding [tiira/*tiira-base-uri* uri]
                       (let [result-ok? (tiira/store-map-border test-borders)]
                         (is result-ok?)
                         ))))

(deftest find-next-page-num-test
  (testing "No link"
    (let [page (tiira/find-next-page-num (read-html-res "result.html"))]
      (is (nil? page))))
  (testing "One link"
    (let [page (tiira/find-next-page-num (read-html-res "result_multipage.html"))]
      (is (= "1" page))))
  (testing "Two links"
    (let [page (tiira/find-next-page-num (read-html-res "result_bothnavi.html"))]
      (is (= "3" page))))
  )
(deftest advanced-search-test
  (testing "No results"
    (stub/with-routes! {{:method      :get
                         :path        "/index.php"
                         :form-params {:haku     "Hae"
                                       :toiminto "29"}}
                        (merge default-response {
                                                 :body tiira/no-sightings-string
                                                 })}
                       (binding [tiira/*tiira-base-uri* uri]
                         (let [result (tiira/advanced-search)]
                           (is (= [] result)
                               )))))

  (testing "Single page result"
    (stub/with-routes! {{:method      :get
                         :path        "/index.php"
                         :form-params {:haku     "Hae"
                                       :toiminto "29"}}
                        (merge default-response {:body (read-html-res "result.html")})}
                       (binding [tiira/*tiira-base-uri* uri]
                         (let [result (tiira/advanced-search)]
                           (is (= 16 (count result)))
                           (is (= "Kyhmyjoutsen" (:species (first result))))
                           (is (= "3.5.2022" (:date (second result))))
                           (is (= "Helsinki" (:county (nth result 2))))
                           (is (= "25776242" (:id (nth result 3))))
                           (is (= "Laajasalo, Yliskylänlahti." (:loc-name (nth result 4))))

                           ))))
  (testing "Multi page result"
    (stub/with-routes! {{:method       :get
                         :path         "/index.php"
                         :query-params {:haku     "Hae"
                                        :toiminto "29"
                                        :sivu     "1"
                                        }}
                        (merge default-response {:body (read-html-res "result.html")})
                        :default
                        (merge default-response {:body (read-html-res "result_multipage.html")})
                        }
                       (binding [tiira/*tiira-base-uri* uri]
                         (let [result (tiira/advanced-search)]
                           (is (= 26 (count result)))
                           (is (= "Kyhmyjoutsen" (:species (first result))))
                           (is (= "7.5.2022" (:date (second result))))
                           (is (= "Helsinki" (:county (nth result 2))))
                           (is (= "25776054" (:id (nth result 24))))
                           (is (= "Laajasalo, Yliskylä." (:loc-name (nth result 25))))
                           )))))


(deftest enrich-sighting-test
  (stub/with-routes! {{:method       :get
                       :path         "/selain/naytahavis.php"
                       :query-params {:id sighting-id}}
                      (merge default-response {:body (read-html-res "sighting.html")})
                      }
                     (binding [tiira/*tiira-base-uri* uri]
                       (let [result (tiira/enrich-sighting {:id sighting-id})]
                         (is (= 6674886.0 (:bird-latitude result)))
                         (is (= 379675.0 (:bird-longitude result)))
                         (is (= 6674514.0 (:spotter-latitude result)))
                         (is (= 379059.0 (:spotter-longitude result)))
                         (is (= "05:40-07:10" (:time result)))
                         ))))

(deftest group-by-location-test
  (let [test-sightings [{:id                1
                         :spotter-longitude 123.0
                         :spotter-latitude  123.0
                         :loc-name          "Place 1"}
                        {:id                2
                         :spotter-longitude 123.0
                         :spotter-latitude  123.0
                         :loc-name          "Place 1"}
                        {:id                3
                         :spotter-longitude 126.0
                         :spotter-latitude  123.0
                         :loc-name          "Place 2"}]
        result (tiira/group-by-location test-sightings)]
    (is (= 2 (count result)))
    (is (= "Place 1" (:loc-name (first result))))    ; Suspicious to assume the order of results
    (is (= 2 (count (:sightings (first result))))
    (is (= "Place 2" (:loc-name (second result))))
)))
