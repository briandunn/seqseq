(ns seqseq.routes
  (:require
    [secretary.core :as sec :include-macros true :refer-macros [defroute]]
    [re-frame.core :refer [dispatch]]
    [goog.events :as events]
    [goog.history.EventType :as EventType])
  (:import goog.History))

(sec/set-config! :prefix "#")

(enable-console-print!)

(defn visit [route]
  (.replace (.-location js/window) route))

(defroute song-new "/songs/new" []
  (dispatch [:add-song]))

(defroute song "/songs/:id" {:as song}
  (dispatch [:set-current-song (int (:id song))]))

(defroute part "/songs/:song-id/parts/:part-id" {:as part}
  (dispatch [:set-current-part part]))

(defroute root "/" []
  (dispatch [:set-current-song nil]))

(comment let [history (History.)
      navigation EventType/NAVIGATE]
  (goog.events/listen history
                      navigation
                      #(-> % .-token sec/dispatch!))
  (.setEnabled history true)
  (.setToken history (.getToken history)))
