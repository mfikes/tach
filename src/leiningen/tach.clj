(ns leiningen.tach
  (:require
    [clojure.string :as string]
    [clojure.java.shell :as shell]
    [leiningen.core.main :as main]
    [leiningen.core.classpath :as classpath]))

(def exit-code-failed-test 1)

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

(defn make-require-test-runner-main-snippet
  [test-runner-main]
  `(require '~test-runner-main))

(defn make-require-cljs-test-snippet
  []
  `(require 'cljs.test))

(defn make-require-planck-core-snippet
  [planck?]
  (when planck? `(require 'planck.core)))

(defn make-inject-exit-handler-snippet
  [planck?]
  `(do (defmethod cljs.test/report [:cljs.test/default :end-run-tests] [~'m]
         (when-not (cljs.test/successful? ~'m)
           ~(if planck?
              `(planck.core/exit ~exit-code-failed-test)
              `(.exit js/process ~exit-code-failed-test))))
       nil))

(defn render-forms-lists
  [& forms-lists]
  (string/join " " (filter some? (apply concat forms-lists))))

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

(defn get-source-paths
  [project build]
  (or (get-in project [:tach :source-paths])
      (get-in build [:source-paths])))

(defn tach-debug?
  [project]
  (get-in project [:tach :debug?]))

(defn tach-force-non-zero-exit-on-test-failure?
  [project]
  (get-in project [:tach :force-non-zero-exit-on-test-failure?]))

(defn tach-cache?
  [project]
  (get-in project [:tach :cache?]))

(defn exit
  [code message]
  (println message)
  (main/exit code))

(defn tach
  [project & args]
  (let [build (get-build project args)
        _ (when (tach-debug? project)
            (println "Using this build: " (pr-str build)))
        test-runner-ns (get-test-runner-ns project build)
        source-paths (get-source-paths project build)]
    (when-not test-runner-ns
      (throw (ex-info "Failed to determine test runner namespace" {})))
    (let [execution-environment (get-execution-environment args)
          planck? (= execution-environment "planck")
          command-line (concat
                         [execution-environment
                          "-q"
                          "-c" (-> project filtered-classpath (concat source-paths) render-classpath)]
                         (when (tach-cache? project)
                           (if-let [cache-path (get-in project [:tach :cache-path])]
                             ["--cache" cache-path]
                             ["--auto-cache"]))
                         ["-e" (render-forms-lists
                                 (when (tach-force-non-zero-exit-on-test-failure? project)
                                   [(make-require-planck-core-snippet planck?)
                                    (make-require-cljs-test-snippet)
                                    (make-inject-exit-handler-snippet planck?)])
                                 [(make-require-test-runner-main-snippet test-runner-ns)])])
          _ (when (tach-debug? project)
              (apply println "Running\n" command-line))
          result (apply shell/sh command-line)]
      (exit (:exit result)
        (str (:out result) (:err result))))))
