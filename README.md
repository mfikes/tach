# Tach

Leiningen plugin for testing libs targeting self-hosted ClojureScript, using either [Lumo](https://github.com/anmonteiro/lumo) or [Planck](http://planck-repl.org).

# Usage

Add `lein-tach` to your Leiningen `:plugins` vector.

[![Clojars Project](https://img.shields.io/clojars/v/lein-tach.svg)](https://clojars.org/lein-tach)

Run via

```
lein tach {self-host-env}

lein tach {self-host-env} {build-id}
```

The `self-host-env` can be either `lumo` or `planck`.

If specified, `{build-id}` is used to identify the `:cljsbuild` build. (Otherwise the first is used.)

The `:cljsbuild` build is used to extract:

- source paths (it uses the `:source-paths` value)
- the main test runner namespace (assumed to be the one listed under `:main`)

You can override the main test runner namespace by including configuration like the following in your `project.clj`:

```
{:tach {:test-runner-ns 'my.test-runner}}
```

In order to enable verbose logging, include config like the following in `project.clj`:

```
{:tach {:debug? true}}
```

In order to force non-zero exit code on test failure, include config like the following in `project.clj`:

```
{:tach {:force-non-zero-exit-on-test-failure? true}}
```

# Example

Here is Tach using Lumo to run [Andare](https://github.com/mfikes/andare)'s unit tests (the only change needed was to add the Tach plugin to Andare's `project.clj`):

```
$ lein tach lumo

Testing cljs.core.async.pipeline-test
nil

Testing cljs.core.async.buffer-tests

Testing cljs.core.async.tests

Ran 42 tests containing 132 assertions.
0 failures, 0 errors.
```

# License

Copyright © 2017 Mike Fikes and Contributors

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
