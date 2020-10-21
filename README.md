# Tach

Leiningen plugin for testing libraries targeting self-hosted ClojureScript.

## Usage

Add `lein-tach` to your Leiningen `:plugins` vector.

[![Clojars Project](https://img.shields.io/clojars/v/lein-tach.svg)](https://clojars.org/lein-tach)

Then

```
lein tach lumo
```

or

```
lein tach planck
```

will cause Tach to execute your unit tests in [Lumo](https://github.com/anmonteiro/lumo) or [Planck](http://planck-repl.org).

## Example

```
$ lein tach lumo

Testing my-lib.core-test

Ran 42 tests containing 132 assertions.
0 failures, 0 errors.
```

## Configuration

Tach looks in your `project.clj` and extracts the following from the `:cljsbuild` configuration:

- source paths (it uses the `:source-paths` value)
- the main test runner namespace (assumed to be the one listed under `:main`)

If needed, you can specify the `:cljsbuild` build identifier as the final argument as in:

```
lein tach lumo dev
```

You can explicitly set any of the Tach configuration values by including configuration like the following in your `project.clj` (all of the keys are optional):

```
:tach {:test-runner-ns 'my-lib.test-runner
       :source-paths ["src/cljs" "src/cljc"]
       :force-non-zero-exit-on-test-failure? true
       :cache? true
       :cache-path "/custom/path/to/cache_dir"
       :debug? true}
```

If either `:test-runner-ns` or `:source-paths`is specified, it overrides any value derived from `:cljsbuild`.

The `:force-non-zero-exit-on-test-failure?` flag can be useful for CI builds: If set to `true`, it causes code like the following to be evaluated, which causes Lumo or Planck to exit with a non-zero code upon test failures:

```clojure
(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (self-host-env-specific-exit 1)))
```

If you'd like to enable Lumo or Planck compilation caching, include `cache? true` and if you'd like to specify the directory to be used for caching, specify it via `:cache-path`.

You can enable Tach diagnostic debug output by setting `:debug? true`.

## License

Copyright © 2017–2020 Mike Fikes and Contributors

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
