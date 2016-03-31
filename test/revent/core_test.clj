(ns revent.core-test
  (:require [clojure.core.async :refer [go chan <! >! <!! ] :as async]
            [clojure.test       :refer :all]
            [revent.core        :as revent]))

(deftest full-stack-test
  (testing "A complete revent system"
    (let [out (chan)
          in (async/to-chan [1 2 3])
          system (revent/system
                  :topic-fn (constantly :all)
                  :inputs [in]
                  :outputs {:log out}
                  :state {:accu (atom 0)}
                  :handlers {:all [+
                                   :on :accu
                                   :to :log]})]
      (is (= 1 (<!! out)))
      (is (= 3 (<!! out)))
      (is (= 6 (<!! out))))))
