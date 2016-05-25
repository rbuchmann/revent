(ns revent.core
  (:require [clojure.core.async :refer [go <! >! chan] :as async]
            [revent.streams     :as streams]))

(def to-pipe streams/to-pipe)

(def to-sink streams/to-sink)

(def to-source streams/to-source)

(defn make-system []
  #{})

(defn add-path [system & items]
  (into system (partition 2 1 items)))

(defn add-paths [system & paths]
  (reduce (partial apply add-path) system paths))

(def conj-set (fnil conj #{}))

(defn collect-links [edges]
  (reduce
   (fn [m [a b]]
     (-> m
         (update-in [a :out]
                    conj-set b)
         (update-in [b :in]
                    conj-set a)))
   {}
   edges))

(defn maybe-substitute [k provide default]
  (if (keyword? k)
    (get provide k default)
    k))

(defn implementation-map [links provide missing-fn]
  (into {}
        (for [[k {:keys [in out]}] links
              :let [sub (partial maybe-substitute k provide)]]
          [k (condp = 0
               (count in) (to-source (sub (missing-fn :source)))
               (count out) (to-sink (sub (missing-fn :sink)))
               (to-pipe (sub (missing-fn :pipe))))])))

(defn nodes [system]
  (->> system
       (apply concat)
       set))

(defn link-all! [links im]
  (doseq [[k {:keys [out]}] links]
    (if (= 1 (count out))
      (async/pipe (im k)
                  (im (first out)))
      (when (> (count out) 0)
        (let [t (async/mult (im k))]
          (doseq [out-chan (map im out)]
            (async/tap t out-chan)))))))

(defn build! [system & {:keys [provide missing-fn]}]
  (let [missing-fn (or missing-fn (fn [k] (chan)))
        links (collect-links system)
        im (implementation-map links provide missing-fn)]
    (link-all! links im)
    {:implementation im
     :system system}))
