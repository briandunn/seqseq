(ns seqseq.handlers-test
  (:require [clojure.test :refer [deftest is]]
            [seqseq.handlers.note :as note]))

(deftest add-note-adds-the-note-to-the-current-part
  (let [db {:current-part-id 25
            :notes {}
            :parts {25 {:beats 1}}
            :quant 1}
        id 47
        note (get-in (note/add db id {:x 0.5 :y 0}) [:notes id])]
    (is (= {:part-id 25 :id 47} (select-keys note [:part-id :id])))
    (is (= {:beat 0 :tick 0} (select-keys note [:beat :tick])))
    ))

(deftest round-to-multiple
  (is (= 6 (note/round-to-multiple 7 2)))
  (is (= 0 (note/round-to-multiple 1 2))))
