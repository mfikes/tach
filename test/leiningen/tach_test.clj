(ns leiningen.tach-test
  (:require
    [clojure.test :refer [deftest is]]
    [leiningen.tach :as tach])
  (:import
    (clojure.lang ExceptionInfo)))

(deftest clojurescript-jar?-test
  (is (tach/clojurescript-jar? "/foo/bar/clojurescript-1.2.3.jar"))
  (is (not (tach/clojurescript-jar? "/foo/bar/fipp-2.3.2.jar"))))

(deftest render-classpath-test
  (is (= "a.jar:b.jar:/foo/src"
         (tach/render-classpath ["a.jar" "b.jar" "/foo/src"]))))

(deftest get-execution-environment-test
  (is (thrown-with-msg? ExceptionInfo #"No execution environment.*"
        (tach/get-execution-environment [])))
  (is (thrown-with-msg? ExceptionInfo #"Execution enviromnents supported: lumo or planck"
        (tach/get-execution-environment ["bogus"])))
  (is (= "lumo" (tach/get-execution-environment ["lumo"])))
  (is (= "lumo" (tach/get-execution-environment ["lumo" "dev"])))
  (is (= "planck" (tach/get-execution-environment ["planck"])))
  (is (= "planck" (tach/get-execution-environment ["planck" "dev"]))))

(deftest render-require-test-runner-main-test
  (is (= "(clojure.core/require (quote foo.core))"
         (tach/render-require-test-runner-main 'foo.core))))

(deftest render-require-cljs-test
  (is (= "(clojure.core/require (quote cljs.test))"
         (tach/render-require-cljs-test))))

(deftest render-require-planck-core-test
  (is (= "(clojure.core/require (quote planck.core))"
         (tach/render-require-planck-core true)))
  (is (= "nil"
         (tach/render-require-planck-core false))))

(deftest render-inject-exit-handler-test
  (is (= "(do (clojure.core/defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m] (clojure.core/when-not (cljs.test/successful? m) (planck.core/exit 1))) nil)"
         (tach/render-inject-exit-handler true)))
  (is (= "(do (clojure.core/defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m] (clojure.core/when-not (cljs.test/successful? m) (.exit js/process 1))) nil)"
         (tach/render-inject-exit-handler false))))

(deftest get-build-test
  (let [project
        '{:cljsbuild
          {:builds
           [{:id "dev"
             :source-paths ["src/test/cljs" "src/main/clojure/cljs"]
             :compiler {:main cljs.core.async.test-runner
                        :asset-path "../out"
                        :optimizations :none
                        :output-to "tests.js"
                        :output-dir "out"}}
            {:id "simple"
             :source-paths ["src/test/cljs" "src/main/clojure/cljs"]
             :compiler {:optimizations :simple
                        :pretty-print true
                        :static-fns true
                        :output-to "tests.js"
                        :output-dir "out-simp"}}]}}]
    (is (= '{:compiler {:asset-path "../out"
                       :main cljs.core.async.test-runner
                       :optimizations :none
                       :output-dir "out"
                       :output-to "tests.js"}
            :id "dev"
            :source-paths ["src/test/cljs"
                           "src/main/clojure/cljs"]}
          (tach/get-build project ["planck"])))
    (is (= '{:id "simple"
             :source-paths ["src/test/cljs" "src/main/clojure/cljs"]
             :compiler {:optimizations :simple
                        :pretty-print true
                        :static-fns true
                        :output-to "tests.js"
                        :output-dir "out-simp"}}
           (tach/get-build project ["planck" "simple"])))))

(deftest unquoted-test
  (is (= 'foo (tach/unquoted 'foo)))
  (is (= 'foo (tach/unquoted ''foo))))

(deftest get-test-runner-ns-test
  (let [project1
        '{:cljsbuild
          {:builds
           [{:id "dev"
             :source-paths ["src/test/cljs" "src/main/clojure/cljs"]
             :compiler {:main cljs.core.async.test-runner
                        :asset-path "../out"
                        :optimizations :none
                        :output-to "tests.js"
                        :output-dir "out"}}]}}
        project2
        '{:cljsbuild
          {:builds
           [{:id "dev"
             :source-paths ["src/test/cljs" "src/main/clojure/cljs"]
             :compiler {:main cljs.core.async.test-runner
                        :asset-path "../out"
                        :optimizations :none
                        :output-to "tests.js"
                        :output-dir "out"}}]}
          :tach {:test-runner-ns 'foo.core}}
        build
        '{:id "dev"
          :source-paths ["src/test/cljs" "src/main/clojure/cljs"]
          :compiler {:main cljs.core.async.test-runner
                     :asset-path "../out"
                     :optimizations :none
                     :output-to "tests.js"
                     :output-dir "out"}}]
    (is (= 'cljs.core.async.test-runner
           (tach/get-test-runner-ns project1 build)))
    (is (= 'foo.core
           (tach/get-test-runner-ns project2 build)))))

(deftest get-source-paths-test
  (let [project1
        '{:cljsbuild
          {:builds
           [{:id "dev"
             :source-paths ["src/test/cljs" "src/main/clojure/cljs"]
             :compiler {:main cljs.core.async.test-runner
                        :asset-path "../out"
                        :optimizations :none
                        :output-to "tests.js"
                        :output-dir "out"}}]}}
        project2
        '{:cljsbuild
          {:builds
           [{:id "dev"
             :source-paths ["src/test/cljs" "src/main/clojure/cljs"]
             :compiler {:main cljs.core.async.test-runner
                        :asset-path "../out"
                        :optimizations :none
                        :output-to "tests.js"
                        :output-dir "out"}}]}
          :tach {:source-paths ["abc/def" "foo/bar"]}}
        build
        '{:id "dev"
          :source-paths ["src/test/cljs" "src/main/clojure/cljs"]
          :compiler {:main cljs.core.async.test-runner
                     :asset-path "../out"
                     :optimizations :none
                     :output-to "tests.js"
                     :output-dir "out"}}]
    (is (= ["src/test/cljs" "src/main/clojure/cljs"]
           (tach/get-source-paths project1 build)))
    (is (= ["abc/def" "foo/bar"]
           (tach/get-source-paths project2 build)))))

(deftest tach-debug?-test
  (is (tach/tach-debug? {:tach {:debug? true}}))
  (is (not (tach/tach-debug? {:tach {}}))))

(deftest tach-force-non-zero-exit-on-test-failure?-test
  (is (tach/tach-force-non-zero-exit-on-test-failure? {:tach {:force-non-zero-exit-on-test-failure? true}}))
  (is (not (tach/tach-force-non-zero-exit-on-test-failure? {:tach {}}))))

(deftest tach-cache?-test
  (is (tach/tach-cache? {:tach {:cache? true}}))
  (is (not (tach/tach-cache? {:tach {}}))))

(deftest tach-verbose?-test
  (let [tach-verbose {:tach {:verbose? true :test-runner-ns 'com.some.ns}}
        tach-not-verbose {:tach {}}
        execution-env "planck"]
  (is (tach/tach-verbose? tach-verbose))
  (is (not (tach/tach-verbose? tach-not-verbose)))
  (is (= [execution-env "-v"] (take 2 (tach/build-command-line tach-verbose [execution-env]))))))
