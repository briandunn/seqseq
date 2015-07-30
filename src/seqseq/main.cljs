(ns seqseq.main
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as reagent :refer [atom]]
            [seqseq.transport :as transport]
            [seqseq.routes :as routes]
            [seqseq.components.part :as part-component]
            [seqseq.keyboard :as keyboard]
            [seqseq.subs]
            [re-frame.core :refer [subscribe dispatch]]))

(defn link-to [route child]
  [:a {:href "#" :onClick (fn [e]
                            (.preventDefault e)
                            (routes/visit route))} child])

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
                                      [link-to (routes/part (select-keys part [:id :song-id]))
                                       [part-component/summary part]]]))]])

(defn play-bar [part]
  (let [transport (subscribe [:transport])
        song (subscribe [:current-song])]
    (fn [part]
      (when (not= :stop @transport)
        (let [duration (transport/part->sec @part (:tempo @song))]
          [:div.play-bar {:style {:animation-duration (str duration "s") }}])))))

(def html-id-seq (atom (range)))

(defn- next-html-id []
  (let [id (-> html-id-seq deref first)]
    (reset! html-id-seq (-> html-id-seq deref rest))
    id))

(defn labeled-number-input [opts label]
  (let [id (str "number-input-" (next-html-id))]
    (fn [{:keys [value min max on-change]} label]
      [:dl
       [:dt
        [:label {:for id} label]]
       [:dd
        [:input {:id id
                 :max max
                 :min min
                 :type "number"
                 :value value
                 :on-change #(-> % .-target .-value int on-change)}]]])))

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
         [labeled-number-input {:value (:tempo @song)
                                :max 480
                                :min 1
                                :on-change #(dispatch [:set-tempo %])} "tempo"]
         (when @part
           [:div
            [labeled-number-input {:value (:beats @part)
                                   :max 64
                                   :min 1
                                   :on-change #(dispatch [:set-beats %])} "beats"]])
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
