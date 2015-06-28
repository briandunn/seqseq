(ns seqseq.transport
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [cljs.core.async :as async :refer [put! chan timeout <! >!]]))

(def buffer-time 0.75)
(def progress-interval 50)
(defonce -control-chan (chan))

; (defonce context (js/AudioContext.))
(defonce context nil)
(defn current-time [] (.-currentTime context))

(defn tone [start]
  (let [osc (.createOscillator context)]
    (.connect osc (.-destination context))
    (set! (.. osc -frequency -value) 440)
    (.start osc start)
    (.stop osc (+ start 0.6))))

(def song {:tempo 120
           :parts [{:beats 3
                    :sounds [
                             {:beat 0
                              :tick 0
                              :play tone}
                             {:beat 1
                              :tick 0
                              :play tone}]}]})

(defn fill [from til length start]
  )

(defn song->seconds [song]
  (let [{:keys [tempo parts]} song
        beats-per-sec (/ tempo 60)
        ticks-per-sec (/ beats-per-sec 96)]
    (mapv
      (fn [part]
        {:duration-secs (* beats-per-sec (:beats part))
         :sounds (mapv (fn [sound]
                         {:play (:play sound)
                          :start-secs (+
                                       (* beats-per-sec (:beat sound))
                                       (* ticks-per-sec (:tick sound)))})
                       (:sounds part))})
      (:parts song))))

(defn notes-in-window [song from til]
  "given a from and til in seconds
  returns a vector of {:start sec :play (fn [start] )}"
  (song->seconds song))

(defn -schedule [started-at from til]
  (map (fn [note]
         ((:play note) (+ started-at (:start note)))) (notes-in-window song from til)))

(defn -play [now control-chan]
  (let [started-at (now)]
    (-schedule started-at 0 buffer-time)
    (go-loop [scheduled-until buffer-time]
             (let [[_ port] (async/alts! [(timeout progress-interval) control-chan])]
               (when (not= port control-chan)
                 (let [progress (- (now) started-at)]
                   (if (> (+ progress buffer-time) scheduled-until)
                     (let [new-schedule-end (+ scheduled-until buffer-time)]
                       (-schedule started-at scheduled-until new-schedule-end)
                       (recur new-schedule-end))
                     (recur scheduled-until))))))))

(defn play [] (-play current-time -control-chan))
(defn stop []
  (.log js/console "stop")
  (put! -control-chan :stop))

