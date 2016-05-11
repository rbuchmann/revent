(ns revent.streams
  (:require [clojure.core.async :as async :refer [go <! >! put! take! chan]]))

(defprotocol Source
  (to-source [this]))

(defprotocol Sink
  (to-sink [this]))

(defprotocol Pipe
  (to-pipe [this]))

(defmacro for-chan [[value ch] & body]
  `(async/go-loop []
     (when-some [~value (<! ~ch)]
       ~@body
       (recur))))

(extend-type clojure.core.async.impl.channels.ManyToManyChannel
  Source
  (to-source [this] this)
  Sink
  (to-sink [this] this)
  Pipe
  (to-sink [this] [this this]))

(extend-type clojure.lang.IFn
  Sink
  (to-sink [f]
    (let [in (chan)]
      (for-chan [v in]
                (f v))
      in))
  Source
  (to-source [f]
    (async/to-chan (f)))
  Pipe
  (to-pipe [f]
    (chan 1 (map f))))

(extend-type clojure.lang.IPersistentVector
  Source
  (to-source [this]
    (async/to-chan this)))

(def make-system set)

(defn p-> [& args]
  (if (< (count args) 2)
    nil
    (let [chs (map-indexed (fn [i arg]
                             (condp = i
                               0 (to-source arg)
                               (-> args count dec) (to-sink arg)
                               (to-pipe arg)))
                           args)
          pairs (partition 2 1 chs)]
      (doseq [[from to] pairs]
        (async/pipe from to)))))

(defn atom-sink [f a]
  (partial swap! a f))
