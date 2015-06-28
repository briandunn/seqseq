(ns seqseq.test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [seqseq.transport :as transport]
            ))

(deftest notes-in-window-unroles-loops
  (let [song {:tempo 60
              :parts [{:beats 1
                       :sounds [{:beat 0
                                 :tick 0
                                 :play :a}]}]}
        times (transport/notes-in-window song 0 1)]
    (is (= times [{:start 0 :play :a}]))))

(deftest fill
  (is (= (transport/fill 0 1 1 0) [0]))
  (is (= (transport/fill 0 1 0.5 0) [0 0.5]))
  (is (= (transport/fill 0 1 0.5 0.5) []))
  (is (= (transport/fill 1 2 1 0.25) [1.25]))
  (is (= (transport/fill 1 2 0.25 0) [1 1.25 1.5 1.75]))
  )
