(ns seqseq.routes
  (:require
    [secretary.core :as sec :include-macros true :refer-macros [defroute]]
    [re-frame.core :refer [dispatch]]
    [goog.events :as events]
    [goog.history.EventType :as EventType])
  (:import [goog History]
           [goog.history Html5History]))

(defonce history (new Html5History))

(defn visit [route]
  (.setToken history (apply str (rest route))))

(defroute song-new "/songs/new" []
  (dispatch [:add-song]))

(defroute song "/songs/:id" [id]
  (dispatch [:set-current-song (int id)]))

(defroute part "/parts/:id" [id]
  (dispatch [:set-current-part (int id)]))

(defroute root "/" []
  (dispatch [:set-current-song nil]))

(defroute songs "/songs" []
  (dispatch [:set-current-song nil]))

(defn init []
  (events/removeAll history EventType/KEYPRESS)
  (events/listen history
                 EventType/NAVIGATE
                 (fn [e]
                   (.log js/console (.-token e))
                   (sec/dispatch! (.-token e))))
  (.setUseFragment history false)
  (.setEnabled history true)
  (visit (.. js/window -location -pathname)))
