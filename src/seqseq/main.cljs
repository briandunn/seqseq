(ns seqseq.main
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as reagent :refer [atom]]
            [seqseq.transport :as transport]
            [seqseq.routes :as routes]
            [seqseq.note :refer [coords->note]]
            [seqseq.components.part :as part-component]
            [seqseq.keyboard :as keyboard]
            [seqseq.subs]
            [re-frame.core :refer [subscribe dispatch]]))

(defn link-to [route & children]
  [:a {:href "#" :onClick (fn [e]
                            (.preventDefault e)
                            (routes/visit route))} children])

(defn song-index []
  [:section#songs
   [:button {:on-click #(dispatch [:add-song])} "+"]
   [:ul
    (for [song (deref (subscribe [:songs]))]
      ^{:key (:id song)} [:li.song
                          [link-to (routes/song (select-keys song [:id]))]])]])

(defn stop [state]
  (swap! state assoc :transport :stop)
  (transport/stop))

(defn toggle-selection [song note-path]
  (swap! song update-in note-path (fn [note]
                                    (assoc note :selected? (not (:selected? note))))))

(defn parts [ps]
  [:section#parts
   [:ul (for [part @ps]
          (if (:blank? part)
            ^{:key (:position part)} [:li.empty {:on-click (fn [e]
                                                             (.preventDefault e)
                                                             (dispatch [:add-part (:position part)]))}]
            ^{:key (:position part)} [:li.part
                                      [link-to (routes/part (select-keys part [:id :song-id]))]]))]])

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
        [link-to (routes/songs) "songs"]
        (when @part
          [link-to (routes/song (select-keys @song [:id])) "parts"])
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
         [part-component/edit part (subscribe [:notes]) play-bar]
         [parts (subscribe [:parts])])])))

(defn root []
  [:div
   [:header
    [:h1
     [link-to (routes/root) "seqseq"]]]
   (let [s (subscribe [:current-song])
         p (subscribe [:current-part])]
     (if @s
       [song s p]
       [song-index]))])

(reagent/render [root]
                (js/document.getElementById "app"))

(defn init []
  ; init db
  (dispatch [:initialise-db])
  ; load router
  (routes/init)
  ; listen to the keyboard
  (keyboard/init))

(init)
