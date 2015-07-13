(ns seqseq.test
  (:require-macros [cemerick.cljs.test :refer [is deftest run-tests testing done]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cemerick.cljs.test]
            [cljs.core.async :as async :refer [timeout chan put! take! <!]]
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
  (is (= (transport/fill 0 1 1 1) []))
  (is (= (transport/fill 0 1 0.5 0) [0 0.5]))
  (is (= (transport/fill 0 1 0.5 0.5) [0.5 1]))
  (is (= (transport/fill 1 2 1 0.25) [1.25]))
  (is (= (transport/fill 1 2 0.25 0) [1 1.25 1.5 1.75])))

(deftest ^:async play
  (let [song-chan (chan)
        time (atom 0)
        now (fn [] @time)
        scheduled-sounds (atom [])]
    (put! song-chan {:tempo 60
                     :parts [{:beats 1
                              :sounds [{:beat 0
                                        :tick 0
                                        :play (fn [start]
                                                (swap! scheduled-sounds conj start))}]}]})
    (go
      (transport/play song-chan now)
      (is (= [] @scheduled-sounds))
      (<! (timeout 51))
      (is (= [0] @scheduled-sounds))
      (reset! time 1)
      (<! (timeout 51))
      (is (= [0 1] @scheduled-sounds))
      (done)
      )))
