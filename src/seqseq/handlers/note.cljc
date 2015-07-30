; vim: set ft=clojure:
(ns seqseq.handlers.note
  (:require
    [seqseq.note :refer [pitch-count ticks-per-beat]]))

(defn round-to-multiple [x base]
  (* base (-> (/ x base) Math/floor int)))

(defn quant-ticks [q]
  (-> (/ ticks-per-beat q) int))

(defn quantize-start [x beats quant]
  (round-to-multiple
    (Math/round (* beats ticks-per-beat x))
    (quant-ticks quant)))

(defn coords->note [start-ticks y]
  (let [tick (int (mod start-ticks ticks-per-beat))
        beat (int (/ (- start-ticks tick) ticks-per-beat))
        pitch (- (Math/ceil (* (- 1 y) pitch-count)) 1)]
    {:beat beat
     :tick tick
     :pitch pitch}))

(defn add [db id coords]
  (let [part-id (:current-part-id db)
        quant (:quant db)]
    (assoc-in db [:notes id] (merge
                               (coords->note
                                 (quantize-start
                                   (:x coords)
                                   (get-in db [:parts part-id :beats])
                                   quant)
                                 (:y coords))
                               {:part-id part-id
                                :duration (quant-ticks quant)
                                :id id}))))
