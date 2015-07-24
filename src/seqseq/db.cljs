(ns seqseq.db (:require [cljs.reader]
                        [schema.core :as s :include-macros true]))

(def schema {:songs (s/both PersistentTreeMap {s/Int {:id s/Int :tempo s/Int}})
             :parts (s/both PersistentTreeMap {s/Int {:id s/Int :song-id s/Int :beats s/Int :position s/Int}})
             :notes (s/both PersistentTreeMap {s/Int {:id s/Int :part-id s/Int :beat s/Int :tick s/Int :duration s/Int :pitch s/Int}})
             :current-song-id (s/maybe s/Int)
             :current-part-id (s/maybe s/Int)
             :transport (s/enum :stop :play :pause)})

(def default-value {:songs (sorted-map)
                    :parts (sorted-map)
                    :notes (sorted-map)
                    :current-song-id nil
                    :current-part-id nil
                    :transport :stop })

(def lsk "seqseq")

(defn ls->songs
  "Read in songs from LS, and process into a map we can merge into app-db."
  []
  (let [data (some->> (.getItem js/localStorage lsk) (cljs.reader/read-string))]
    (merge data {:songs (into (sorted-map) (:songs data))
                 :parts (into (sorted-map) (:parts data))
                 :notes (into (sorted-map) (:notes data))})))

(defn songs->ls!
  "Puts songs into localStorage"
  [songs]
  (.setItem js/localStorage lsk (str songs)))
