(ns manifold-cljs.stream.async
  (:require [clojure.core.async :as a]
            [manifold-cljs.deferred :as d]
            [manifold-cljs.stream.core :as s])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]))

;; - AtomicReference -> atom
(s/def-source CoreAsyncSource
  [ch
   last-take]

  :stream
  [(isSynchronous [_] false)

   (description [this]
     {:source? true
      :drained? (s/drained? this)
      :type "core.async"})

   (close [_]
     (a/close! ch))]

  :source
  [(take [this default-val blocking?]
     (assert (not blocking?) "Blocking takes on Core.Async source not supported!")
     (let [d  (d/deferred)
           d' @last-take
           _  (reset! last-take d)
           f  (fn [_]
                (a/take! ch
                  (fn [msg]
                    (d/success! d
                      (if (nil? msg)
                        (do
                          (s/markDrained this)
                          default-val)
                        msg)))))]
       (if (d/realized? d')
         (f nil)
         (d/on-realized d' f f))
       d))

   (take [this default-val blocking? timeout timeout-val]
     (assert (not blocking?) "Blocking takes on Core.Async source not supported!")
     (let [d  (d/deferred)
           d' @last-take
           _  (reset! last-take d)

           ;; if I don't take this out of the goroutine, core.async OOMs on compilation
           mark-drained #(s/markDrained this)
           f  (fn [_]
                (go
                  (let [result (alt!
                                 ch ([x] (if (nil? x)
                                           (do
                                             (mark-drained)
                                             default-val)
                                           x))
                                 (a/timeout timeout) timeout-val
                                 :priority true)]
                    (d/success! d result))))]
       (if (d/realized? d')
         (f nil)
         (d/on-realized d' f f))
       d))])

;; - AtomicReference -> atom
;; - remove `blocking?` branches
(s/def-sink CoreAsyncSink
  [ch
   last-put]

  :stream
  [(isSynchronous [_] false)

   (description [this]
     {:sink? true
      :closed? (s/closed? this)
      :type "core.async"})

   (close [this]
     (if (s/closed? this)
       false
       (do
         (s/markClosed this)
         (let [d @last-put
               f (fn [_] (a/close! ch))]
           (d/on-realized d
             (fn [_] (a/close! ch))
             nil)
           true))))]

  :sink
  [(put [this x blocking?]
     (assert (not blocking?) "Blocking puts on core.async sink not supported!")
     (assert (not (nil? x)) "core.async channel cannot take `nil` as a message")

     (cond
       (s/closed? this)
       (d/success-deferred false)

       :else
       (let [d  (d/deferred)
             d' @last-put
             _  (reset! last-put d)
             f  (fn [_]
                  (go
                    (d/success! d
                      (boolean
                        (a/>! ch x)))))]
         (if (d/realized? d')
           (f nil)
           (d/on-realized d' f f))
         d)))

   (put [this x blocking? timeout timeout-val]
     (assert (not blocking?) "Blocking puts on core.async sink not supported!")

        (if (nil? timeout)
          (s/put this x blocking?)
          (assert (not (nil? x)) "core.async channel cannot take `nil` as a message"))

        (if (s/closed? this)
          (d/success-deferred false)

          (let [d  (d/deferred)
                d' @last-put
                _  (reset! last-put d)
                f  (fn [_]
                     (go
                       (let [result (alt!
                                      [[ch x]] true
                                      (a/timeout timeout) timeout-val
                                      :priority true)]
                         (d/success! d result))))]
            (if (d/realized? d')
              (f nil)
              (d/on-realized d' f f))
            d)))])

(extend-type cljs.core.async.impl.channels.ManyToManyChannel
  s/Sinkable
  (to-sink [ch]
    (->CoreAsyncSink
      ch
      (atom (d/success-deferred true)))))

(extend-type cljs.core.async.impl.channels.ManyToManyChannel
  s/Sourceable
  (to-source [ch]
    (->CoreAsyncSource
      ch
      (atom (d/success-deferred true)))))
