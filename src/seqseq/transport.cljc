; vim: set ft=clojure:
(ns seqseq.transport
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go-loop go]]))
  (:require
    #?@(:clj [[clojure.core.async :as async :refer [go go-loop put! chan timeout <! >! close!]]]
        :cljs [[cljs.core.async   :as async :refer [put! chan timeout <! >! close!]]
               [seqseq.synth      :as synth :refer [current-time tone]]]))
  )

(defn fill [from til duration-secs start-secs]
  (let [from (* duration-secs (Math/floor (/ from duration-secs)))]
    (loop [times [] now from]
      (if (< (+ start-secs now) til)
        (recur (conj times (+ now start-secs)) (+ now duration-secs))
        times))))

(defn part->sec [part tempo]
  (* (:beats part) (/ 60 tempo)))

(defn song->seconds [song]
  (let [{:keys [tempo parts]} song
        secs-per-beat (/ 60 tempo)
        secs-per-tick (/ secs-per-beat 96)]
    (mapv
      (fn [part i]
        (let [part-duration #(part->sec part tempo)]
          {:duration-secs (part-duration)
           :sounds (mapv (fn [sound]
                           (assoc sound
                                  :start-secs (+
                                               (* secs-per-beat (:beat sound))
                                               (* secs-per-tick (:tick sound)))
                                  :duration   (* secs-per-tick (:duration sound))))
                         (:sounds part))}))
      (:parts song) (range))))

(defn notes-in-window [song from til]
  "given a from and til in seconds
  returns a vector of {:start sec :play (fn [start] )}"
  (filter #(>= (:start %) from)
          (flatten
            (map
              (fn [{:keys [duration-secs sounds]}]
                (map (fn [sound]
                       (map (fn [start] (assoc sound :start start))
                            (fill from til duration-secs (:start-secs sound)))) sounds))
              (song->seconds song)))))

(defn -schedule [song-chan started-at from til]
  "reads the song from its chan.
   enqueues the notes between from and til.
   pushes beat events into the chan."
  (go
    (let [notes (notes-in-window (<! song-chan) from til)]
      (doseq [note notes]
        (let [start (+ started-at (:start note))]
          (tone start (dissoc note :play :beat :tick)))))))

(defonce -control-chan (chan))

(defn -play [song-chan now]
  (let [started-at (now)
        buffer-time 0.50
        progress-interval 50]
    (-schedule song-chan started-at 0 buffer-time)
    (go-loop [scheduled-until buffer-time]
             (let [[op _] (async/alts! [(timeout progress-interval)
                                            -control-chan])]
               (if (not= :stop op)
                 (let [progress (- (now) started-at)]
                   (if (> (+ progress buffer-time) scheduled-until)
                     (let [new-schedule-end (+ scheduled-until buffer-time)]
                       (-schedule song-chan started-at scheduled-until new-schedule-end)
                       (recur new-schedule-end))
                     (recur scheduled-until)))
                 (close! song-chan))))))

(defn play
  ([song-chan]
   (play song-chan current-time))
  ([song-chan now]
   (-play song-chan now)))

(defn stop []
  (put! -control-chan :stop))
