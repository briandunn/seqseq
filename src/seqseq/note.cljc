; vim: set ft=clojure:
(ns seqseq.note)

(def pitch-count 88)
(def ticks-per-beat 96)

(def pitch-names ["C" "C#" "D" "D#" "E" "F" "F#" "G" "G#" "A" "A#" "B"])
(def pitches (reverse (map (fn [i]
                             (let [name (nth pitch-names (mod i (count pitch-names)))]
                               {:name (str name i) :num i :sharp (some  (partial = \#) name)}))
                           (range pitch-count))))
