(ns revent.core-test
  (:require [clojure.core.async :refer [go chan <! >! <!! ] :as async]
            [clojure.test       :refer :all]
            [revent.core        :as revent]))

(deftest collect-links-test
  (let [edges [[:a :b] [:b :c] [:b :d]]
        links (revent/collect-links edges)]
    (is (= links {:a {:out #{:b}}
                  :b {:in #{:a}
                      :out #{:c :d}}
                  :c {:in #{:b}}
                  :d {:in #{:b}}}))))

(deftest link-test
  (let [a (chan)
        b (chan)
        c (chan)]
    (revent/link-all! {:a {:out #{:b}} :b {:out #{:c}}}
                      {:a a :b b :c c})
    (async/put! a 1)
    (is (= (<!! c) 1))))

(deftest mult-test
  (let [a (chan)
        b (chan)
        c (chan)]
    (revent/link-all! {:a {:out #{:b :c}}}
                      {:a a :b b :c c})
    (async/put! a 1)
    (is (= (<!! b) 1))
    (is (= (<!! c) 1))))

(deftest build-simple-system
  (let [{:keys [in out] :as m} (revent/build!
                          (revent/add-path (revent/make-system)
                                           :in inc :out))]
    (println (m inc))
    (println "retval!" (async/put! (m inc) 1))
    (is (= (<!! (m inc)) 2))))

(deftest build-system-test
  (let [a (atom 0)
        b (atom 0)
        ch (chan)
        out (chan)
        system (revent/add-paths (revent/make-system)
                                 [:in (partial swap! a +)]
                                 [:in inc (partial swap! b +)]
                                 [:in :out])]
    (revent/build! system :provide {:in ch
                                    :out out})
    (async/put! ch 1)
    (async/put! ch 2)
    (<!! out)
    (<!! out)
    (is (= 3 @a))
    (is (= 5 @b))))
