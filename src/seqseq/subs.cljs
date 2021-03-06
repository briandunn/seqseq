(ns seqseq.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub subscribe dispatch]]
            [seqseq.transport :as transport]
            [seqseq.synth  :refer [tone current-time]]))

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
  (fn [db _] (let [songs (reaction (vals (:songs @db)))]
               (reaction
                 (doall
                   (map
                     (fn [song]
                       (assoc song :notes (flatten
                                            (doall
                                              (map
                                                (fn [part]
                                                  (doall
                                                    (map (fn [note]
                                                           (merge note (select-keys part [:position :beats])))
                                                         (part-notes @db (:id part)))))
                                                (song-parts @db (:id song)))))))
                     @songs))))))

(defn loop-tick [started-at]
  {:beat 0
   :tick 1
   :duration 0
   :pitch 0
   :play (fn [start _ &]
           (js/setTimeout
             #(dispatch [:update-position])
             (* (- start (current-time)) 1000)))})

(register-sub
  :song-feed
  (fn [db _]
    (let [song (subscribe [:current-song])
          parts (reaction (song-parts @db (:id @song)))]
      (reaction {:tempo (:tempo @song)
                 :parts (map (fn [part]
                               (assoc
                                 (select-keys part [:beats :id])
                                 :sounds
                                 (conj
                                   (map
                                     (fn [note] (assoc note :play tone))
                                     (part-notes @db (:id part))) (loop-tick (get-in @db [:transport :started-at])))))
                             (remove :muted? @parts))} ))))

(defn round-to-multiple [x base]
  (* base (Math/floor (/ x base))))

(register-sub
  :play-head-position
  (fn [db [_ part-id]]
    (let [song  (subscribe [:current-song])
          beats (reaction (get-in @db [:parts part-id :beats]))
          tempo (reaction (:tempo @song))
          now   (reaction (get-in @db [:transport :position]))
          part-duration (reaction (transport/beats->secs @beats @tempo))
          p (reaction
              (/
               (- @now (round-to-multiple @now @part-duration)) @part-duration))]
      (reaction
        [@p (* (- 1 @p) @part-duration)]))))

(register-sub
  :transport
  (fn [db _] (reaction (get-in @db [:transport :state]))))

(register-sub
  :quant
  (fn [db _] (reaction (:quant @db))))
