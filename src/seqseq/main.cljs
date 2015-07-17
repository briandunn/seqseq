(ns seqseq.main
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as reagent :refer [atom]]
            [seqseq.transport :as transport]
            [seqseq.routes :as routes]
            [seqseq.synth :as synth]
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

(defonce context (new js/AudioContext))
(defn current-time [] (.-currentTime context))

(defn play [state song]
  (swap! state assoc :transport :play)
  (let [song-chan (chan)]
    (transport/play song-chan current-time)
    (go-loop []
             (when (>! song-chan @song) (recur)))))

(defn stop [state]
  (swap! state assoc :transport :stop)
  (transport/stop))

(defn play-bar [duration]
  [:div.play-bar {:style {:animation-duration (str duration "s") }}])

(def pitch-names ["C" "C#" "D" "D#" "E" "F" "F#" "G" "G#" "A" "A#" "B"])

(def pitches (reverse (map (fn [i]
                             (let [name (nth pitch-names (mod i (count pitch-names)))]
                               {:name (str name i) :num i :sharp (some  (partial = \#) name) }
                               )
                             ) (range 88))))

(defn f->% [f]
  (str (* 100 f) "%"))

(defn note->style [note beats]
  (let [{:keys [tick beat pitch duration]} note
        pitches (count pitches)
        part-ticks (* 96 beats) ]
    {:left (f->% (/ (+ tick (* 96 beat)) part-ticks))
       :top  (str "calc(" (f->% (/ (- pitches pitch 1) pitches)) " + 3px)")
       :width (f->% (/ duration part-ticks))}))

(defn part [song part-name]
  (let [part (get-in @song [:parts 0])
        beats (:beats part)
        key-list pitches]
    [:section#piano-roll
     [:section#grid
      (when (not= :stop (:transport @app-state))
        [play-bar (transport/part->sec part (:tempo @song))])
      [:div.measures
       (for [m (range beats)]
         ^{:key m} [:div.measure {:style {:width (str (/ 100.0 beats) "%")}}])]
      [:ul.notes (for [n (:sounds part)]
                   ^{:key n}[:li {:style (note->style n beats) }])]
      [:ul
       (for [k key-list]
         ^{:key (:num k)} [:li.row {:class (when (:sharp k) "sharp")}])]]
     [:ul#keyboard (for [k key-list]
                     ^{:key (:num k)} [:li.row {:class (when (:sharp k) "sharp")} (:name k)] )]]))

(defn parts [names]
  [:section#parts
   [:ul (for [part names]
          ^{:key part} [:li.part
                        [:a {:href (routes/part {:id part})}]])]])

(def tone (partial synth/tone context))

(defn song-new [state]
  (let [song (atom {:tempo 120
                    :parts [
                            {:beats 2
                             :sounds [{:beat 0
                                       :tick 0
                                       :pitch 57
                                       :duration 48
                                       :play tone}
                                      {:beat 1
                                       :tick 0
                                       :pitch 59
                                       :duration 24
                                       :play tone}
                                      {:beat 1
                                       :tick 48
                                       :pitch 60
                                       :duration 12
                                       :play tone}]}]})]
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

(defn init []
  ; listen for route changes
  (let [route-chan (routes/init)]
    (go-loop []
             (when-let [route (<! route-chan)]
               (swap! app-state assoc :nav route)
               (recur))))

  ; init transport
  (transport/init))

(init)
