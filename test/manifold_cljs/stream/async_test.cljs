(ns manifold-cljs.stream.async-test
  (:require [manifold-cljs.stream :as s]
            [manifold-cljs.stream.core :as core]
            [manifold-cljs.deferred :as d]
            [clojure.core.async :as a]
            [cljs.test :refer [deftest testing is async]]

            [manifold-cljs.stream.async]))

(defn later* [f]
  (js/setTimeout f 0))

(defn no-success? [d]
  (= ::none (d/success-value d ::none)))

(deftest async-stream
  (async done
         (let [c (a/chan)
               src (s/->source c)
               snk (s/->sink c)
               p1 (s/put! snk 1)
               t1 (s/take! src)]
           (is (no-success? p1))
           (is (no-success? t1))
           (later*
             ;; differently to "native" Manifold streams,
             ;; the put doesn't get ACK'd immediately
             (fn []
               (is @p1)
               (is (= 1 @t1))
               (done))))))
