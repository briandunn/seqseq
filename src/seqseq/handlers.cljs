(ns seqseq.handlers (:require
                      [seqseq.db     :refer [default-value ls->songs songs->ls! schema]]
                      [seqseq.note   :refer [coords->note]]
                      [schema.core   :as s]
                      [re-frame.core :refer [register-handler path trim-v after]]))

(defn check-and-throw
      "throw an exception if db doesn't match the schema."
      [a-schema db]
      (if-let [problems  (s/check a-schema db)]
         (throw (js/Error. (str "schema check failed: " problems)))))

(def check-schema (after (partial check-and-throw schema)))

(def ->ls (after songs->ls!))

(def log-middleware (after (fn [db] (.log js/console (clj->js db)))))

(defn allocate-next-id
  "Returns the next id.
  Returns one more than the current largest id."
  [col]
  ((fnil inc 0) (last (keys col))))

(def new-song-template {:tempo 120})

(def new-part-template {:beats 4})

(defn p [args &] (.log js/console (clj->js args)) (first args))

(register-handler
  :initialise-db
  check-schema
  (fn [_ _]
    (merge default-value (p (ls->songs)))))

(register-handler
  :add-song
  check-schema
  (fn [db]
    (let [id (allocate-next-id (:songs db))]
      (assoc-in
        (assoc db :current-song-id id)
        [:songs id] (merge {:id id} new-song-template)))))



(register-handler
  :add-part
  [check-schema ->ls]
  (fn [db [_ position]]
    (let [id (allocate-next-id (:parts db))]
      (assoc-in
        (assoc db :current-part-id id)
        [:parts id] (merge {:id id :position position :song-id (:current-song-id db)} new-part-template)))))

(register-handler
  :set-current-song
  check-schema
  (fn [db [_ song]]
    (merge db {:current-song-id song :current-part-id nil})))

(register-handler
  :set-current-part
  check-schema
  (fn [db [_ id]]
    (assoc db :current-part-id id)))

(register-handler
  :set-tempo
  check-schema
  (fn [db [_ tempo]]
    (assoc-in db [:songs (:current-song-id db) :tempo] tempo)))

(register-handler
  :play
  check-schema
  (fn [db [_ _]]
    (assoc db :transport :play)))

(register-handler
  :stop
  check-schema
  (fn [db [_ _]]
    (assoc db :transport :stop)))

(register-handler
  :add-note
  [check-schema ->ls]
  (fn [db [_ coords]]
    (let [id (allocate-next-id (:notes db))
          part-id (:current-part-id db)
          beats (get-in db [:parts part-id :beats])]
      (assoc-in db [:notes id] (merge (coords->note coords beats) {:part-id part-id :id id})))))
