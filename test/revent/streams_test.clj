(ns revent.streams-test
  (:require [clojure.test       :as t :refer [deftest is]]
            [revent.streams     :as streams]
            [clojure.core.async :refer [<!!] :as async]))

(deftest for-chan
  (let [a (atom [])
        ch (async/to-chan [1 2 3])]
    (<!! (streams/for-chan [v ch]
                           (swap! a conj v)))
    (is (= [1 2 3] @a))))

(deftest basic-streams
  (is (= [1 2 3] (let [a (atom [])]
                   (streams/p-> [1 2 3] #(do (println %)
                                             (swap! a conj %)))
                   (Thread/sleep 500)
                   @a))))
