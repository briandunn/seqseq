(ns seqseq.handlers
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [seqseq.db     :refer [default-value ls->songs songs->ls! schema]]
    [schema.core   :as s]
    [seqseq.routes :as routes :refer [visit]]
    [seqseq.transport :as transport]
    [seqseq.handlers.note :as note]
    [cljs.core.async :as async :refer [chan >! <!]]
    [re-frame.core :refer [register-handler after subscribe dispatch]]
    [re-frame.handlers :refer [register-base]]
    [reagent.core :refer [next-tick]]
    [re-frame.middleware :as mw]))

(defn check-and-throw
  "throw an exception if db doesn't match the schema."
  [a-schema db]
  (if-let [problems  (s/check a-schema db)]
    (throw (js/Error. (str "schema check failed: " problems)))))

(def check-schema (after (partial check-and-throw schema)))

(def ->ls (after songs->ls!))

(defn allocate-next-id
  "Returns the next id.
  Returns one more than the current largest id."
  [col]
  ((fnil inc 0) (last (keys col))))

(def new-song-template {:tempo 120})

(def new-part-template {:beats 4})

(register-handler
  :initialise-db
  check-schema
  (fn [_ _]
    (merge default-value (ls->songs))))

(register-handler
  :add-song
  [ check-schema
   (after (fn [db]
            (visit (routes/song {:id (last (keys (:songs db)))}))))]
  (fn [db]
    (let [id (allocate-next-id (:songs db))]
      (assoc-in
        (assoc db :current-song-id id)
        [:songs id] (merge {:id id} new-song-template)))))

(register-handler
  :add-part
  [check-schema ->ls (after (fn [db]
                              (visit (routes/part {:id (last (keys (:parts db)))}))))]
  (fn [db [_ position]]
    (let [id (allocate-next-id (:parts db))]
      (assoc-in
        db
        [:parts id]
        (merge
          {:id id
           :position position
           :muted? false
           :song-id (:current-song-id db)}
          new-part-template)))))

(register-handler
  :set-current-song
  check-schema
  (fn [db [_ song]]
    (merge db {:current-song-id song :current-part-id nil})))

(register-handler
  :set-current-part
  check-schema
  (fn [db [_ id]]
    (merge db {:current-part-id id
               :current-song-id (or (get-in db [:parts id :song-id]) (:current-song-id db))})))

(register-handler
  :set-tempo
  [check-schema ->ls]
  (fn [db [_ tempo]]
    (assoc-in db [:songs (:current-song-id db) :tempo] tempo)))

(register-handler
  :set-beats
  [check-schema ->ls]
  (fn [db [_ beats]]
    (assoc-in db [:parts (:current-part-id db) :beats] beats)))

(register-handler
  :set-quant
  [check-schema]
  (fn [db [_ quant]]
    (assoc db :quant quant)))

(defn now []
  (/ (.. js/window -performance now) 1000))

(register-handler
  :play
  [check-schema]
  (fn [db [_ _]]
    (let [song-chan (chan)
          song (subscribe [:song-feed])]
      (transport/play song-chan)
      (go-loop []
               (when (>! song-chan @song)
                 (recur))))
    (assoc-in db [:transport] {:state :play :position 0.00 :started-at (now)})))

(register-handler
  :update-position
  [check-schema]
  (fn [db [_ _]]
    (let [playing? (= :play (get-in db [:transport :state]))]
      (assoc-in db [:transport :position]
                (if playing? (- (now) (get-in db [:transport :started-at])) 0)))))

(register-handler
  :stop
  check-schema
  (fn [db [_ _]]
    (transport/stop)
    (assoc-in db [:transport] {:state :stop :position 0 :started-at 0})))

(register-handler
  :toggle-selection
  check-schema
  (fn [db [_ note-id]]
    (update-in db [:selection] conj note-id)))

;note: will delete notes in parts you can't see
(register-handler
  :delete-selected-notes
  [check-schema ->ls]
  (fn [db [_ _]]
    (assoc (update-in db [:notes] #(apply dissoc % (vec (:selection db)))) :selection #{})))

(register-handler
  :add-note
  [check-schema (mw/undoable "add note") ->ls]
  (fn [db [_ coords]]
    (note/add db (allocate-next-id (:notes db)) coords)))

(register-handler
  :toggle-mute
  [check-schema (mw/undoable "toggle mute") ->ls]
  (fn [db [_ pos]]
    (if-let [part-id (:id (first
                         (filter (fn [part]
                                   (and
                                     (= (:current-song-id db) (:song-id part))
                                     (= pos (:position part))))
                                 (-> db :parts vals))))]
      (update-in db [:parts part-id] (fn [part]
                                       (merge
                                         part
                                         {:muted? (not (:muted? part))})))
      db)))
