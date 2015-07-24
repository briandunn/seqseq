(ns seqseq.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub subscribe]]))

(register-sub
  :current-song
  (fn [db _] (reaction (get-in @db [:songs (:current-song-id @db)]))))

(register-sub
  :current-part
  (fn [db _] (reaction (get-in @db [:parts (:current-part-id @db)]))))

(defn p [args &] (.log js/console (clj->js args)) (first args))

(register-sub
  :parts
  (fn [db _]
    (let [parts (filter
                  (fn [part]
                    (= (:song-id part) (:current-song-id @db)))
                  (vals (:parts @db)))]
      (reaction (doall (map (fn [n]
                              (or
                                (first (filter (fn [part] (= n (:position part))) parts))
                                {:position n :blank? true}))
                            (range 8)))))))

(register-sub
  :notes
  (fn [db _]
    (reaction (map
                #(assoc % :selected? ((:selection @db) (:id %)))
                (filter
                  #(= (:part-id %) (:current-part-id @db))
                  (-> @db :notes vals))))))

(register-sub
  :songs
  (fn [db _] (reaction (vals (:songs @db)))))

(register-sub
  :transport
  (fn [db _] (reaction (:transport @db))))
