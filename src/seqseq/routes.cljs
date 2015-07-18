(ns seqseq.routes
  (:require
    [secretary.core :as sec :include-macros true :refer-macros [defroute]]
    [cljs.core.async :as async :refer [put! chan close!]]
    [goog.events :as events]
    [goog.history.EventType :as EventType])
  (:import goog.History))

(sec/set-config! :prefix "#")

(defonce -chan (chan))

(defn visit [route]
  (.replace (.-location js/window) route))

(defroute song-new "/songs/new" []
  (put! -chan [:song-new]))

(defroute part "/songs/new/parts/:id" {:as part}
  (put! -chan [:song-new (:id part)]))

(defroute root "/" []
  (put! -chan [:song-index]))

(let [history (History.)
      navigation EventType/NAVIGATE]
  (goog.events/listen history
                      navigation
                      #(-> % .-token sec/dispatch!))
  (.setEnabled history true)
  (.setToken history (.getToken history)))

(defn init [] -chan)
