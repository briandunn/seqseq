(ns seqseq.test
  (:require [clojure.test :refer [deftest is]]
            [clojure.core.async :as async :refer [go-loop timeout chan >! <!!]]
            [seqseq.transport :as transport]
            ))

(deftest notes-in-window-unroles-loops
  (let [song {:tempo 60
              :parts [{:beats 1
                       :sounds [{:beat 0
                                 :tick 0
                                 :duration 0
                                 :play :a}]}]}
        times (transport/notes-in-window song 0 1)]
    (is (= [{:start 0.0}] (map #(select-keys % [:start]) times)))))

(deftest fill
  (is (= [0.0] (transport/fill 0 1 1 0)))
  (is (= [] (transport/fill 0 1 1 1)))
  (is (= [0.0 0.5] (transport/fill 0 1 0.5 0)))
  (is (= [0.5] (transport/fill 0 1 0.5 0.5)))
  (is (= [1.25] (transport/fill 1 2 1 0.25)))
  (is (= [1.0 1.25 1.5 1.75] (transport/fill 1 2 0.25 0))))

(deftest play
  (let [song-chan (chan)
        time (atom 0)
        now #(deref time)
        scheduled-sounds (atom [])]
    (go-loop []
             (>! song-chan {:tempo 60
                            :parts [{:beats 1
                                     :sounds [{:beat 0
                                               :tick 0
                                               :duration 0
                                               :play (fn [start &params]
                                                       (swap! scheduled-sounds conj start))}]}]})
             (recur))
    (transport/play song-chan now)
    (is (= [] @scheduled-sounds))
    (<!! (timeout 51))
    (is (= [0.0] @scheduled-sounds))
    (reset! time 0.49)
    (<!! (timeout 51))
    (reset! time 0.99)
    (<!! (timeout 51))
    (reset! time 1.49)
    (<!! (timeout 51))
    (is (= [0.0 1.0] @scheduled-sounds))))

