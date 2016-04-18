(ns revent.streams-test
  (:require [clojure.test   :as t :refer [deftest is]]
            [revent.streams :as streams]))


(deftest basic-streams
  (is (= [1 2 3] (let [a (atom [])]
                   (streams/p-> [1 2 3] (streams/atom-sink conj a))
                   (Thread/sleep 50)
                   @a))))
