(ns seqseq.main
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [seqseq.transport :as transport]
            [seqseq.routes :as routes]
            [cljs.core.async :as async :refer [put! chan <! >!]]))

(enable-console-print!)

(defonce app-state (atom {:songs [], :nav :song-index}))

(defn song-index [_ _]
  (reify
    om/IRender
    (render [_]
      (dom/section #js {:id "songs"}
                   (dom/button #js {:onClick (fn [e] (.preventDefault e)
                                               (routes/visit (routes/song-new))
                                               )} "+" )))))

(defn song-new [_ state]
  (reify
    om/IRender
    (render [_] (dom/section #js {:className "controls" }
                             (dom/h2 nil "sequence")
                             (dom/a #js {:href (routes/root)} "songs")
                             (dom/a #js {:href "#"} "parts")
                             (dom/article nil
                                          (dom/section nil
                                                       (dom/div #js {:id "transport"}
                                                                (dom/button #js {:onClick (fn [e]
                                                                                            (.preventDefault e)
                                                                                            (transport/play))}
                                                                            "►"))))))))

(defn root [state parent]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/header nil (dom/h1 nil (dom/a #js {:href (routes/root)} "seqseq")))
               (apply om/build ((:nav state) {:song-new [song-new state] :song-index [song-index (:songs state)]}))))))

(om/root root app-state {:target (. js/document (getElementById "app")) })

(go-loop []
  (swap! app-state assoc :nav (<! routes/chan))
  (recur))
