(ns seqseq.main
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as reagent :refer [atom]]
            [seqseq.transport :as transport]
            [seqseq.routes :as routes]
            [cljs.core.async :as async :refer [chan <! >!]]))

(enable-console-print!)

(defonce app-state (atom {:songs []
                          :nav [:song-index]
                          :transport :stop}))

(defn song-index [state]
  [:section#songs
   [:button {:on-click (fn [e]
                         (.preventDefault e)
                         (routes/visit (routes/song-new)))}
    "+"]])

(defn play [state song]
  (swap! state assoc :transport :play)
  (let [song-chan (chan)]
  (transport/play song-chan)
  (go-loop []
           (>! song-chan @song) (recur))))

(defn stop [state]
  (swap! state assoc :transport :stop)
  (transport/stop))

(defonce position (atom {:beat 0 :start 0}))

(defn play-bar [beats]
  (let [{:keys [beat start]} @position
        progress (* 100 (/ (+ beat 1) beats))]
    (.log js/console beat start)
    [:div.play-bar {:style {:transition-duration (str start "s")
                            :left (str progress "%") }}]))

(go-loop []
         (let [beat (<! transport/pulse)]
           (reset! position beat))
         (recur))

(defn part [song part-name]
  (let [part (get-in @song [:parts 0])
        beats (:beats part)
        key-list (range 88)]
    [:section#piano-roll
     [:section#grid
      [play-bar beats]
      [:div.measures
       (for [m (range beats)]
         ^{:key m} [:div.measure {:style {:width (str (/ 100.0 beats) "%")}}])]
      [:ul.notes (for [n (:sounds part)]
                   [:li {:style {:left (str (* 100 (/
                                                    (+ (:tick n) (* 96 (:beat n)))
                                                    (* 96 beats) )) "%")
                                 :width "4%" }}])]
      [:ul
       (for [k key-list]
         ^{:key k} [:li.row])]]
     [:ul#keyboard (for [k key-list]
                     ^{:key k} [:li.row k] )]]))

(defn parts [names]
  [:section#parts
   [:ul (for [part names]
          ^{:key part} [:li.part
                        [:a {:href (routes/part {:id part})}]])]])

(defn song-new [state]
  (let [song (atom {:tempo 120
                    :parts [
                            {:beats 2
                             :sounds [{:beat 0
                                       :tick 0
                                       :play transport/tone}
                                      {:beat 1
                                       :tick 0
                                       :play transport/tone}
                                      {:beat 1
                                       :tick 48
                                       :play transport/tone}]}]})]
    (fn [state]
      [:dev
       [:section.controls
        [:h2 "sequence"]
        [:a {:href (routes/root)} "songs"]
        [:a {:href (routes/song-new)} "parts"]
        [:article
         [:dl
          [:dt
           [:label {:for "tempo"} "tempo"]]
          [:dd
           [:input#tempo {:max 480
                          :min 1
                          :type "number"
                          :value (:tempo @song)
                          :on-change (fn [e]
                                       (swap! song assoc :tempo (.. e -target -value))
                                       )}]]
          [:dt
           [:label {:for "beats"} "beats"]
           [:dd
            [:input#beats {:max 64
                           :min 1
                           :type "number"
                           :value (get-in @song [:parts 0 :beats])
                           :on-change (fn [e]
                                        (swap! song assoc-in [:parts 0 :beats] (.. e -target -value))
                                        )}]]]]
         [:section
          [:div#transport
           (if (= :play (:transport @state))
             [:button {:on-click (fn [e] (stop state))} "◼︎" ]
             [:button {:on-click (fn [e] (play state song))} "►"]
             )]]]]
       (let [[_ part-name] (:nav @state)
             part-names ["Q" "W" "E" "R" "A" "S" "D" "F"]
             ]
         (if part-name
           [part song part-name]
           [parts part-names]))])))

(defn root [props]
  [:div
   [:header
    [:h1
     [:a {:href (routes/root)} "seqseq"]]]
   (let [page-component ((get-in @app-state [:nav 0]) {:song-index song-index
                                       :song-new song-new})]
     [page-component app-state]
     )])

(reagent/render [root]
                (js/document.getElementById "app"))

(go-loop []
         (swap! app-state assoc :nav (<! routes/chan))
         (recur))

(:transport @app-state)
