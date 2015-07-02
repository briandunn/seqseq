(ns seqseq.main
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as reagent :refer [atom]]
            [seqseq.transport :as transport]
            [seqseq.routes :as routes]
            [cljs.core.async :as async :refer [put! chan <! >!]]))

(enable-console-print!)

(defonce app-state (atom {:songs [], :nav :song-index}))

(defn song-index []
  [:section#songs
   [:button {:on-click (fn [e]
                         (.preventDefault e)
                         (routes/visit (routes/song-new)))}
    "+"]])

(defn song-new []
  [:section.controls
   [:h2 "sequence"]
   [:a {:href (routes/root)} "songs"]
   [:a {:href "#"} "parts"]
   [:article
    [:section
     [:div#transport
      [:button {:on-click (fn [e]
                            (.preventDefault e)
                            (transport/play))} "►"]]]]])

(defn root []
  [:div
   [:header
    [:h1
     [:a {:href (routes/root)} "seqseq"]]]
   [((:nav @app-state) {:song-index song-index :song-new song-new})]])

(reagent/render [root]
                (js/document.getElementById "app"))

(go-loop []
         (swap! app-state assoc :nav (<! routes/chan))
         (recur))
