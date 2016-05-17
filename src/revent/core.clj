(ns revent.core
  (:require [clojure.core.async :refer [go <! >! chan] :as async]))

(defn make-system []
  {:nodes #{}
   :edges #{}})

(defn add-path [system & items]
  (-> system
      (update :edges into (partition 2 1 items))
      (update :nodes into items)))

(defn add-paths [system & paths]
  (reduce (partial apply add-path) system paths))

(def sconj (fnil conj #{}))

(defn collect-links [edges]
  (reduce
   (fn [m [a b]]
     (-> m
         (update-in [a :out]
                    sconj b)
         (update-in [b :in]
                    sconj a)))
   {}
   edges))

(defn build! [system & {:keys [provide missing-fn]}]
  (let [missing-fn (or missing-fn (fn [_] (chan)))
        {:keys [nodes edges]} system
        substitute-map (into {}
                             (for [needed (filter keyword? nodes)]
                               [needed (get provide
                                            needed
                                            (missing-fn needed))]))
        links (collect-links edges)]
    links))
