(ns seqseq.transport
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [cljs.core.async :as async :refer [put! chan timeout <! >!]]))

(defonce -control-chan (chan))

(defonce context (js/AudioContext.))

(defn current-time [] (.-currentTime context))

(defn tone [start]
  (let [osc (.createOscillator context) ]
    (.connect osc (.-destination context))
    (set! (.. osc -frequency -value) 440)
    (.start osc start)
    (.stop osc (+ start 0.05))))

(def song {:tempo 120
           :parts [
                   {:beats 2
                    :sounds [{:beat 0
                              :tick 0
                              :play tone}
                             {:beat 1
                              :tick 0
                              :play tone}
                             {:beat 1
                              :tick 48
                              :play tone}]}
                   {:beats 4
                    :sounds [{:beat 1
                              :tick 0
                              :play tone}]}]})

(defn fill [from til duration-secs start-secs]
  (let [from (* duration-secs (Math/floor (/ from duration-secs)))]
    (loop [times [] now from]
      (if (< now til)
        (recur (conj times (+ now start-secs)) (+ now duration-secs))
        times))))

(defn song->seconds [song]
  (let [{:keys [tempo parts]} song
        secs-per-beat (/ 60 tempo)
        secs-per-tick (/ secs-per-beat 96)]
    (mapv
      (fn [part]
        {:duration-secs (* secs-per-beat (:beats part))
         :sounds (mapv (fn [sound]
                         {:play (:play sound)
                          :start-secs (+
                                       (* secs-per-beat (:beat sound))
                                       (* secs-per-tick (:tick sound)))})
                       (:sounds part))})
      (:parts song))))

(defn notes-in-window [song from til]
  "given a from and til in seconds
  returns a vector of {:start sec :play (fn [start] )}"
  (filter #(> (:start %) from)
          (flatten
            (map
              (fn [{:keys [duration-secs sounds]}]
                (map (fn [{:keys [start-secs play] }]
                       (map (fn [start] {:start start :play play})
                            (fill from til duration-secs start-secs))) sounds))
              (song->seconds song)))))

(defn -schedule [started-at from til]
  (comment .log js/console (- (current-time) started-at) from til (< (- (current-time) started-at) from))
  (let [notes (notes-in-window song from til)]
    (comment .log js/console "scheduling " (count notes))
    (doseq [note notes]
      (let [start (+ started-at (:start note))]
        ((:play note) start)))))

(go-loop []
         (when (= :play (<! -control-chan))
           (let [started-at (current-time)
                 buffer-time 0.50
                 progress-interval 50]
             (-schedule started-at 0 buffer-time)
             (loop [scheduled-until buffer-time]
               (when (not= :stop (first (async/alts! [(timeout progress-interval)
                                                      -control-chan])))
                 (let [progress (- (current-time) started-at)]
                   (if (> (+ progress buffer-time) scheduled-until)
                     (let [new-schedule-end (+ scheduled-until buffer-time)]
                       (-schedule started-at scheduled-until new-schedule-end)
                       (recur new-schedule-end))
                     (recur scheduled-until)))))))
         (recur))

(defn play []
  (put! -control-chan :play))

(defn stop []
  (put! -control-chan :stop))
