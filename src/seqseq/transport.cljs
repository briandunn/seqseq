(ns seqseq.transport
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
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

(defn fill [from til duration-secs start-secs]
  (let [from (* duration-secs (Math/floor (/ from duration-secs)))]
    (loop [times [] now from]
      (if (< now (+ now duration-secs))
        (recur (conj times (+ now start-secs)) (+ now duration-secs))
        times))))

(defonce pulse (chan))

(defn song->seconds [song]
  (let [{:keys [tempo parts]} song
        secs-per-beat (/ 60 tempo)
        secs-per-tick (/ secs-per-beat 96)]
    (mapv
      (fn [part i]
        {:duration-secs (* secs-per-beat (:beats part))
         :sounds (concat (mapv (fn [sound]
                                 {:play (:play sound)
                                  :start-secs (+
                                               (* secs-per-beat (:beat sound))
                                               (* secs-per-tick (:tick sound)))})
                               (:sounds part))
                         (mapv (fn [beat]
                                 {:play (fn [start]
                                          (let [ secs-from-now #(- start (current-time))]
                                            (go
                                              (<! (timeout (- (* (- (secs-from-now) secs-per-beat) 1000) 150)))
                                              (put! pulse {:beat beat
                                                           :part i
                                                           :start (secs-from-now)}))))
                                  :start-secs (* secs-per-beat beat)})
                               (range (:beats part))))})
      (:parts song) (range))))

(defn notes-in-window [song from til]
  "given a from and til in seconds
  returns a vector of {:start sec :play (fn [start] )}"
  (filter #(>= (:start %) from)
          (flatten
            (map
              (fn [{:keys [duration-secs sounds]}]
                (map (fn [{:keys [start-secs play] }]
                       (map (fn [start] {:start start :play play})
                            (fill from til duration-secs start-secs))) sounds))
              (song->seconds song)))))

(defn -schedule [song-chan started-at from til]
  "reads the song from its chan.
   enqueues the notes between from and til.
   pushes beat events into the chan."
  (go
    (let [notes (notes-in-window (<! song-chan) from til)]
      (doseq [note notes]
        (let [start (+ started-at (:start note))]
          ((:play note) start))))))

(go-loop []
         (let [[op song-chan] (<! -control-chan)]
           (when (= :play op)
             (let [started-at (current-time)
                   buffer-time 0.50
                   progress-interval 50]
               (-schedule song-chan started-at 0 buffer-time)
               (loop [scheduled-until buffer-time]
                 (let [[[op _] _] (async/alts! [(timeout progress-interval)
                                               -control-chan])]
                   (when (not= :stop op)
                     (let [progress (- (current-time) started-at)]
                       (if (> (+ progress buffer-time) scheduled-until)
                         (let [new-schedule-end (+ scheduled-until buffer-time)]
                           (-schedule song-chan started-at scheduled-until new-schedule-end)
                           (recur new-schedule-end))
                         (recur scheduled-until)))))))))
         (recur))

(defn play [song-chan]
  (put! -control-chan [:play song-chan]))

(defn stop []
  (put! -control-chan [:stop]))
