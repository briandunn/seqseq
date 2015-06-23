(ns seqseq.transport
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [cljs.core.async :as async :refer [put! chan timeout <! >!]]))

(def buffer-time 0.75)
(def progress-interval 50)
(defonce -control-chan (chan))

(defonce context (js/AudioContext.))

(defn -play [context control-chan]
  (let [started-at (.-currentTime context)
        schedule #(.log js/console %1 %2 %3 (< %1 %2))]
    (schedule 0, 0, buffer-time)
    (go-loop [scheduled-until buffer-time]
             (let [[_ port] (async/alts! [(timeout progress-interval) control-chan])]
               (when (not= port control-chan)
                 (let [progress (- (.-currentTime context) started-at)]
                   (if (> (+ progress buffer-time) scheduled-until)
                     (let [new-schedule-end (+ scheduled-until buffer-time)]
                       (schedule progress scheduled-until new-schedule-end)
                       (recur new-schedule-end))
                     (recur scheduled-until))))))))

(defn play [] (-play context -control-chan))
(defn stop []
  (.log js/console "stop")
  (put! -control-chan :stop))

