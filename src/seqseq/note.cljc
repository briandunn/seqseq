; vim: set ft=clojure:
(ns seqseq.note)

(def pitch-names ["C" "C#" "D" "D#" "E" "F" "F#" "G" "G#" "A" "A#" "B"])
(def pitches (reverse (map (fn [i]
                             (let [name (nth pitch-names (mod i (count pitch-names)))]
                               {:name (str name i) :num i :sharp (some  (partial = \#) name)}))
                           (range 88))))

(defn coords->note [coords beats]
  (let [total-ticks (.round js/Math (* beats 96 (:x coords)))
        tick (mod total-ticks 96)
        beat (/ (- total-ticks tick) 96)
        pitch (- (.ceil js/Math (* (- 1 (:y coords)) (count pitches))) 1)]
    {:beat beat
     :tick tick
     :pitch pitch
     :duration 12}))
