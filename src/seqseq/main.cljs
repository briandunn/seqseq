(ns seqseq.main
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as reagent :refer [atom]]
            [seqseq.transport :as transport]
            [seqseq.routes :as routes]
            [seqseq.note :refer [coords->note]]
            [seqseq.components.part :as part-component]
            [seqseq.subs]
            [goog.events :as events]
            [re-frame.core :refer [subscribe dispatch]])
  (:import [goog.events EventType]))

(defn song-index []
  [:section#songs
   [:button {:on-click #(dispatch [:add-song])} "+"]
   [:ul
    (for [song (deref (subscribe [:songs]))]
      ^{:key (:id song)} [:li.song
                          [:a {:href "#" :onClick (fn [e]
                                                    (.preventDefault e)
                                                    (dispatch [:set-current-song (:id song)]))}]])]])

(defn stop [state]
  (swap! state assoc :transport :stop)
  (transport/stop))

(defn toggle-selection [song note-path]
  (swap! song update-in note-path (fn [note]
                                    (assoc note :selected? (not (:selected? note))))))

(defn parts [ps]
  [:section#parts
   [:ul (map (fn [part]
               (if (:blank? part)
                 ^{:key (:position part)} [:li.empty {:on-click (fn [e]
                                                                  (.preventDefault e)
                                                                  (dispatch [:add-part (:position part)]))}]
                 ^{:key (:position part)} [:li.part  {:on-click (fn [e]
                                                                  (.preventDefault e)
                                                                  (dispatch [:set-current-part (:id part)]))}
                                           [:a [part-component/summary part]]])) @ps)]])

(defn play-bar [part]
  (let [transport (subscribe [:transport])
        song (subscribe [:current-song])]
    (fn [part]
      (when (not= :stop @transport)
        (let [duration (transport/part->sec @part (:tempo @song))]
          [:div.play-bar {:style {:animation-duration (str duration "s") }}])))))

(defn song [song part]
  (let [transport (subscribe [:transport])]
    (fn [song part]
      [:dev
       [:section.controls
        [:h2 "sequence"]
        [:a {:href "#" :onClick (fn [e]
                                  (.preventDefault e)
                                  (dispatch [:set-current-song nil])) } "songs"]
        [:a {:href "#" :onClick (fn [e]
                                  (.preventDefault e)
                                  (dispatch [:set-current-part nil])) } "parts"]
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
                                       (dispatch [:set-tempo
                                                  (-> e .-target .-value int)]))}]]]
         (when @part
           [:dl
            [:dt
             [:label {:for "beats"} "beats"]]
            [:dd
             [:input#beats {:max 64
                            :min 1
                            :type "number"
                            :value (:beats @part)
                            :on-change (fn [e]
                                         (dispatch [:set-beats
                                                    (-> e .-target .-value int)]))}]]])
         [:section
          [:div#transport
           (if (= :play @transport)
             [:button {:on-click #(dispatch [:stop])} "◼︎"]
             [:button {:on-click #(dispatch [:play])} "►"]
             )]]]]
       (if @part
         [part-component/edit part (subscribe [:notes]) play-bar {:on-note-click (fn [note-index]
                                                              (dispatch [:toggle-selection note-index]))
                                             :on-note-add (fn [coords]
                                                            (dispatch [:add-note coords]))}]
         [parts (subscribe [:parts])])])))

(defn root []
  [:div
   [:header
    [:h1
     [:a {:href "#"
          :on-click (fn [e]
                      (.preventDefault e)
                      (dispatch [:set-current-song nil]))}
      "seqseq"]]]
   (let [s (subscribe [:current-song])
         p (subscribe [:current-part])]
     (if @s
       [song s p]
       [song-index]))])

(reagent/render [root]
                (js/document.getElementById "app"))

(defn handle-key-press [k e]
  (when (= "x" k)
    (dispatch [:delete-selected-notes]))

  (when (= " " k)
    (.preventDefault e)
    (.stopPropagation e)
    (((deref (subscribe [:transport]))
      {:stop
       #(dispatch [:play])
       :play
       #(dispatch [:stop])
       }))))

(defn init []
  ; init db
  (dispatch [:initialise-db])
  ; load route
  (routes/init)

  ; listen to the keyboard
  (let [keyup (fn [e] (handle-key-press (.fromCharCode js/String (.-keyCode e)) e))]
    (events/removeAll js/window EventType.KEYPRESS)
    (events/listen js/window EventType.KEYPRESS keyup)))
(init)
