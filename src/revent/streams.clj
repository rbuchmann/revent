(ns revent.streams
  (:require [clojure.core.async :as async :refer [go <! >! put! take! chan]]))

(defprotocol EventStreamer
  (pipe! [this target])
  (done! [this]))

(defprotocol EventAcceptor
  (fire! [this event]))

(defmacro for-chan [[value ch] & body]
  `(async/go-loop []
     (when-some [~value (<! ~ch)]
       ~@body
       (recur)))) ;; TODO: Test if this still works in new async

(extend-type clojure.core.async.impl.channels.ManyToManyChannel
  EventStreamer
  (pipe! [this target]
    (for-chan [v this]
              (fire! target v)))
  (done! [this] (async/close! this))
  EventAcceptor
  (fire! [this event]
    (put! this event)))

(extend-type clojure.lang.IFn
  EventAcceptor
  (fire! [f event]
    (f event)))

(extend-type clojure.lang.IPersistentVector
  EventStreamer
  (pipe! [s target]
    (go
      (doseq [item s]
        (fire! target item))))
  (done! [s]))

(defn p-> [& args]
  (let [pairs (partition 2 1 args)]
    (doseq [[from to] pairs]
      (pipe! from to))))

(defn map-pipe [f]
  (let [ch (chan)]
    (reify
      EventStreamer
      (pipe! [_ to] (pipe! ch to))
      (done! [_] (done! ch))
      EventAcceptor
      (fire! [_ event]
        (fire! ch (f event))))))

(defn atom-sink [a f]
  (partial swap! a f))
