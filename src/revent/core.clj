(ns revent.core
  (:require [clojure.core.async :refer [go <! >! chan] :as async]))

(defn mix-all [mix chans]
  (doseq [chan chans]
    (async/admix mix chan))
  mix)

(defn wrap-handler [[handler-fn & {:keys [on to]}]]
  (let [f (if on
            (partial swap! on handler-fn)
            handler-fn)]
    (if to
      (fn [event]
        (>! to (f event)))
      f)))

(defn subscribe-handlers [pub stop handlers]
  (doseq [[topic handler] handlers]
    (let [f (wrap-handler handler)
          events (chan)]
      (async/sub pub topic events)
      (async/go-loop []
        (let [[val port] (async/alts!
                           [stop
                            events]
                           :priority true)]
          (when-not (= val ::stop)
            (f val)
            (recur)))))))

(defn topic-chan [pub topic]
  (let [ch (chan)]
    (async/sub pub topic ch)
    ch))

(defn stop [{:keys [control]}]
  (async/put! control ::stop))

(defn inject-handler-deps [{:keys [state outputs]} handlers]
  (into {} (for [[k [f & {:as opts}]] handlers]
             (let [updated (-> opts
                               (update :on state)
                               (update :to outputs))]
               [k (apply vector f (-> updated seq flatten))]))))

(defn system [& {:keys [inputs outputs topic-fn state handlers] :as system-info}]
  (let [event-chan (chan)
        mix (async/mix event-chan)
        event-pub (async/pub event-chan (or topic-fn :topic))
        control (chan)
        control-pub (async/pub control :topic)]
    (mix-all mix inputs)
    (subscribe-handlers event-pub
                        (topic-chan control-pub :stop)
                        (inject-handler-deps system-info handlers))
    (assoc system-info
           :control control
           :mix mix)))
