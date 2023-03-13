(ns test
  "Prep and launch for bb and jvm tests"
  (:require
   ;[babashka.cli :as cli]
   [clojure.string :as string]
   [helper.shell :as shell]
   [lread.status-line :as status]))

(defn- str-coll [coll]
  (string/join ", " coll))

(defn- opts->args [opts]
  (->> (select-keys opts [:nses :patterns :vars])
       (reduce (fn [acc [k v]]
                 (apply conj acc k v))
               [])))

(defn- prep [args]
  (let [opts {:browsers ["chrome"], :suites [" api"]} #_(parse-opts args)]
    (let [browsers         (:browsers opts)
          virtual-display? (:launch-virtual-display opts)
          suites           (set (:suites opts))
          env              (cond-> {}
                             (seq browsers)
                             (assoc "ETAOIN_TEST_DRIVERS" (mapv keyword browsers)
                                    "ETAOIN_IDE_TEST_DRIVERS" (mapv keyword browsers))
                             virtual-display?
                             (assoc "DISPLAY" ":99.0"))
          shell-opts       (if (seq env)
                             {:extra-env env}
                             {})
          test-runner-args (cond-> (opts->args opts)
                             (suites "api") (concat ["--patterns" "etaoin.api.*-test$"])
                             (suites "ide") (concat ["--nses" "etaoin.ide-test"])
                             (suites "unit") (concat ["--patterns" ".*unit.*-test$"])
                             :always vec)]
      (status/line :head "Running tests")
      (status/line :detail "suites: %s" (if (seq suites) (str-coll (sort suites)) "<none specified>"))
      (status/line :detail "browsers: %s" (if (seq browsers) (str-coll (sort browsers)) "<defaults>"))
      (status/line :detail "runner-args: %s" test-runner-args)

      {:shell-opts shell-opts :test-runner-args test-runner-args})))

;; Entry points

(defn test-bb [& args]
  (when-let [{:keys [shell-opts test-runner-args]} (prep args)]
    (println shell-opts)
    (println test-runner-args)
    (apply shell/command shell-opts "bb -test:bb" test-runner-args)))

(defn test-jvm [& args]
  (when-let [{:keys [shell-opts test-runner-args]} (prep args)]
    (apply shell/clojure shell-opts "-M:test" test-runner-args)))

