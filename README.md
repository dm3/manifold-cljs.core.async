# manifold-cljs/core.async

[![Build Status](https://travis-ci.org/dm3/manifold-cljs.core.async.png?branch=master)](https://travis-ci.org/dm3/manifold-cljs.core.async)

An adapter for converting [Core.Async](https://github.com/clojure/core.async) channels into
[Manifold-cljs](https://github.com/dm3/manifold-cljs) streams.

Clojurescript projects can't have optional dependencies, so this module exists
on its own.

## Usage

Add the following dependency to your project.clj or build.boot:

```clojure
[manifold-cljs/core.async "0.1.6-0"]
```

Then use it in your project:

```clojure
(ns example.project
  (:require [manifold-cljs.stream :as s]
            [clojure.core.async :as a]))

(def chan (a/chan))
(def source-stream (s/->source chan))
(def sink-stream (s/->sink chan))
```

## License

Copyright © 2016 Zach Tellman, Vadim Platonov

Distributed under the MIT License.
