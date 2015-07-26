(ns seqseq.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub subscribe]]))

(defn- part-notes [db part-id]
  (filter
    #(= (:part-id %) part-id)
    (-> db :notes vals)))

(defn- song-parts [db song-id]
  (filter
    #(= (:song-id %) song-id)
    (-> db :parts vals)))

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
    (let [parts (song-parts @db (:current-song-id @db))]
      (reaction (doall (map (fn [n]
                              (or
                                (first (filter (fn [part]
                                                 (= n (:position part))) parts))
                                {:position n :blank? true}))
                            (range 8)))))))

(register-sub
  :notes
  (fn [db [_ part]]
    (let [part-id (or (:id part) (:current-part-id @db))]
      (reaction (doall (map
                         #(assoc % :selected? ((:selection @db) (:id %)))
                         (part-notes @db part-id)))))))

(register-sub
  :songs
  (fn [db _] (reaction (vals (:songs @db)))))

(register-sub
  :song-feed
  (fn [db _]
    (let [song (subscribe [:current-song])
          parts (reaction (song-parts @db (:id @song)))]
      (reaction {:tempo (:tempo @song)
                 :parts (map (fn [part]
                               {:beats (:beats part)
                                :sounds (part-notes @db (:id part))})
                             @parts)} ))))

(register-sub
  :transport
  (fn [db _] (reaction (:transport @db))))
