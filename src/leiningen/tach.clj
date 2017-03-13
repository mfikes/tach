(ns leiningen.tach
  (:require
    [clojure.string :as string]
    [clojure.java.shell :as shell]
    [leiningen.core.main :as main]
    [leiningen.core.classpath :as classpath]))

(defn clojurescript-jar?
  [jar-name]
  (boolean (re-matches #".*/clojurescript-.*\.jar" jar-name)))

(defn render-classpath
  [cp]
  (str (string/join ":" cp)))

(defn get-execution-environment
  [args]
  (when (empty? args)
    (throw (ex-info "No execution environment specified. Expected lumo or planck." {})))
  (let [execution-environment (first args)]
    (when-not (#{"lumo" "planck"} execution-environment)
      (throw (ex-info "Execution enviromnents supported: lumo or planck" {:execution-environemnt execution-environment})))
    execution-environment))

(defn render-require-test-runner-main
  [test-runner-main]
  (pr-str `(require '~test-runner-main)))

(defn get-build
  [project args]
  (let [builds (get-in project [:cljsbuild :builds])]
    (if-some [build-id (second args)]
      (first (filter (fn [build]
                       (= (:id build) build-id))
               builds))
      (first builds))))

(defn filtered-classpath
  [project]
  (remove clojurescript-jar? (classpath/get-classpath project)))

(defn unquoted
  [sym]
  (if (seq? sym)
    (second sym)
    sym))

(defn get-test-runner-ns
  [project build]
  (unquoted (or (get-in project [:tach :test-runner-ns])
                (get-in build [:compiler :main]))))

(defn tach-debug?
  [project]
  (get-in project [:tach :debug?]))

(defn tach
  [project & args]
  (let [build (get-build project args)
        _ (when (tach-debug? project)
            (println "Using this build: " (pr-str build)))
        test-runner-ns (get-test-runner-ns project build)]
    (when-not test-runner-ns
      (throw (ex-info "Failed to determine test runner namespace" {})))
    (let [command-line [(get-execution-environment args)
                        "-q"
                        "-c" (-> project filtered-classpath (concat (:source-paths build)) render-classpath)
                        "-e" (render-require-test-runner-main test-runner-ns)]
          _ (when (tach-debug? project) 
              (apply println "Running\n" command-line))
          result (apply shell/sh command-line)]
      (if (zero? (:exit result))
        (println (:out result))
        (do
          (apply println "Failed to execute:" command-line)
          (prn result)
          (main/exit (:exit result)))))))
