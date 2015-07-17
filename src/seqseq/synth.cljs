(ns seqseq.synth)

(defn pitch->freq [pitch]
 (* 27.50 (Math/pow 2 (/ (- pitch 9) 12))))

(defn tone [context start {:keys [duration pitch]}]
  (let [osc (.createOscillator context) ]
    (.connect osc (.-destination context))
    (set! (.. osc -frequency -value) (pitch->freq pitch))
    (.start osc start)
    (.stop osc (+ start duration))))
