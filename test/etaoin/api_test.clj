(ns etaoin.api-test
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [etaoin.api :as e]
   [etaoin.impl.util :as util]
   [etaoin.test-report :as test-report]
   [slingshot.slingshot :refer [try+]]))

(defn numeric? [val]
  (or (instance? Double val)
      (instance? Integer val)))

;; By default we run the tests with all the drivers supported on the current OS.
;; To override this, you can set the environment variable ETAOIN_TEST_DRIVERS
;; to a Clojure vector encoded as a string; see script/test.clj for how we use this.

(defn get-drivers-from-env []
  (when-let [override (System/getenv "ETAOIN_TEST_DRIVERS")]
    (edn/read-string override)))

(defn os-name []
  (first (str/split (System/getProperty "os.name") #"\s+")))

(defn get-drivers-from-prop []
  (case (os-name)
    "Linux" [:firefox :chrome]
    "Mac" [:chrome :edge :firefox :safari]
    "Windows" [:firefox :chrome :edge]
    nil))

(defn get-default-drivers []
  [:firefox :chrome :safari])

(def default-opts
  {:chrome  {:args ["--no-sandbox"]}
   :firefox {}
   :safari  {}
   :edge    {:args ["--headless"]}})

(def drivers
  (or (get-drivers-from-env)
      (get-drivers-from-prop)
      (get-default-drivers)))

(def ^:dynamic *driver*)


(def url "https://www.wikipedia.org")
;(def url (-> "html/test.html" io/resource str))

;; tests failed in safari 13.1.1 https://bugs.webkit.org/show_bug.cgi?id=202589 use STP newest
(defn fixture-browsers [f]
  (let [url url]
    (doseq [type drivers
            :let [opts (get default-opts type {})]]
      (e/with-driver type opts driver
                     (e/go driver url)

                     ;(e/wait-visible driver {:id :document-end})
                     (e/wait-visible driver {:tag :input :name :search})

                     (binding [*driver*              driver
                               test-report/*context* (name type)]
                       (testing (name type)
                         (f)))))))

(use-fixtures
 :each
 fixture-browsers)

(defn report-browsers [f]
  (println "Testing with browsers:" drivers)
  (f))

(use-fixtures
 :once
 report-browsers)

(deftest test-url
  (doto *driver*
    (-> e/get-url
        ;(str/ends-with? "/resources/html/test.html")
        (str/includes? "wikipedia" )
        is)))
