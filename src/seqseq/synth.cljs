(ns seqseq.synth
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [timeout <!]]
            [re-frame.handlers :refer [register-base]]))

(defn pitch->freq [pitch]
 (* 27.50 (Math/pow 2 (/ (- pitch 9) 12))))

(def oscilators (atom []))

(def key-downs (atom {}))

(defonce context (new js/AudioContext))
(defn current-time [] (.-currentTime context))

(defn- check-in-osc!
  ([osc]
    (swap! oscilators conj osc))
  ([osc t]
   (go
     (<! (timeout (* (- t (current-time)) 1000)))
     (check-in-osc! osc))))

(defn- create-osc []
  (let [osc (.createOscillator context)
        gain (.createGain context)]
    (set! (.. gain -gain -value) 0)
    (.connect gain (.-destination context))
    (.connect osc gain)
    (.start osc 0)
    {:gain (.. gain -gain) :osc osc}))

(defn- check-out-osc []
  (or
    (when-let [osc (last @oscilators)]
      (swap! oscilators pop)
      osc)
    (create-osc)))

(defn- set-osc-pitch! [osc pitch]
  (.setValueAtTime (.. (:osc osc) -frequency) (pitch->freq pitch) (current-time))
  osc)

(defn play [pitch]
  (let [osc (set-osc-pitch! (check-out-osc) pitch)]
    (swap! key-downs assoc pitch osc)
    (.setValueAtTime (:gain osc) 1 (current-time))))

(defn stop [pitch]
  (when-let [osc (get @key-downs pitch)]
    (swap! key-downs dissoc pitch)
    (.setValueAtTime (:gain osc) 0 (current-time))
    (check-in-osc! osc)))

(defn tone [start {:keys [duration pitch]}]
  (let [osc (check-out-osc)
        gain (:gain osc)]
    (set-osc-pitch! osc pitch)
    (.setValueAtTime gain 0 (- start 0.0005))
    (.linearRampToValueAtTime gain 1 start)
    (let [stop-time (+ start duration)]
      (check-in-osc! osc stop-time)
      (.setValueAtTime gain 1 (- stop-time 0.0005))
      (.linearRampToValueAtTime gain 0 stop-time))))

; register handlers

(register-base
  :play-pitch
  (fn [_ [_ {:keys [num]}]]
    (play num)))

(register-base
  :stop-pitch
  (fn [_ [_ {:keys [num]}]]
    (stop num)))
