(ns seqseq.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :as async :refer [put! chan <! >!]]
            [secretary.core :as sec :include-macros true :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(let [history (History.)
      navigation EventType/NAVIGATE]
  (goog.events/listen history
                      navigation
                      #(-> % .-token sec/dispatch!))
  (doto history (.setEnabled true)))

(sec/set-config! :prefix "#")

(enable-console-print!)

(defonce app-state (atom {:songs [], :nav :song-index}))

(defroute song-new-route "/songs/new" []
  (swap! app-state assoc :nav :song-new))

(defroute root-route "/" []
  (swap! app-state assoc :nav :song-index))

(defonce audio-context (js/AudioContext.))

(defn song-index [_ _]
  (reify
    om/IRender
    (render [_]
      (dom/section #js {:id "songs"}
                   (dom/button #js {:onClick (fn [e] (.preventDefault e)
                                               (.replace (.-location js/window) (song-new-route))
                                               )} "+" )))))

(defn song-new [_ _]
  (reify
    om/IRender
    (render [_] (dom/section #js {:className "controls" }
                             (dom/h2 nil "sequence")
                             (dom/a #js {:href (root-route)} "songs")
                             (dom/a #js {:href "#"} "parts") ))))

(defn root [state parent]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/header nil (dom/h1 nil (dom/a #js {:href (root-route)} "seqseq")))
               (apply om/build ((:nav state) {:song-new [song-new {}] :song-index [song-index (:songs state)]}))))))

(om/root root app-state {:target (. js/document (getElementById "app")) })

(sec/dispatch! "/")
