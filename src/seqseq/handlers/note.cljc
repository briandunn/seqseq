; vim: set ft=clojure:
(ns seqseq.handlers.note
  (:require
    [seqseq.note :refer [pitch-count ticks-per-beat]]))

(defn round-to-multiple [x base]
  (* base (-> (/ x base) Math/floor int)))

(defn quant-ticks [q]
  (-> (/ ticks-per-beat q) int))

(defn- quantize-start [db x]
  (round-to-multiple
    (Math/round (* (get-in db [:parts (:current-part-id db) :beats]) ticks-per-beat x))
    (quant-ticks (:quant db))))

(defn- x->when [db x]
  (let [start-ticks (quantize-start db x)
        tick (int (mod start-ticks ticks-per-beat))]
    {:beat (int (/ (- start-ticks tick) ticks-per-beat))
     :tick tick }))

(defn- y->pitch [y]
  (- (Math/ceil (* (- 1 y) pitch-count)) 1))

(defn- when->ticks [{:keys [beat tick]}] (+ tick (* beat ticks-per-beat)))

(defn add [db id coords]
  (assoc-in db [:notes id] (merge
                             (x->when db (:x coords))
                             {:pitch (y->pitch (:y coords))
                              :part-id (:current-part-id db)
                              :duration (quant-ticks (:quant db))
                              :id id})))

(defn move [db id coords]
  (update-in db [:notes id] merge (assoc
                                    (x->when db (:x coords))
                                    :pitch (y->pitch (:y coords)))))

(defn resize [db id value]
  (update-in db [:notes id] (fn [current]
                              (let [end (when->ticks (x->when db value))
                                    start (when->ticks current)
                                    duration (- end start)
                                    rounded-duration (if (<= duration 0)
                                                       (quant-ticks (:quant db))
                                                       duration)]
                                (assoc current :duration rounded-duration)))))
