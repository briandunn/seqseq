(ns seqseq.components.part (:require [seqseq.note   :refer [pitches]]
                                     [re-frame.core :refer [subscribe dispatch]]))

(defn- f->% [f]
  (str (* 100 f) "%"))

(defn- note->style [note beats]
  (let [{:keys [tick beat pitch duration]} note
        pitches (count pitches)
        part-ticks (* 96 beats) ]
    {:left  (f->% (/ (+ tick (* 96 beat)) part-ticks))
     :top   (f->% (/ (- pitches pitch 1) pitches))
     :width (f->% (/ duration part-ticks))}))

(defn play-bar [part]
  (let [position (subscribe [:play-head-position (:id part)])]
    (fn [part]
      (dispatch [:update-position])
      [:div.play-bar {:style {:left (f->% @position)}}])))

(defn summary [part]
  (let [notes  (deref (subscribe [:notes part]))
        height (f->% (/ 1 (count pitches))) ]
    (fn [part]
      [:ul.notes
       [play-bar part]
       (for [n notes]
         ^{:key (:id n)} [:li {:style (assoc (note->style n (:beats part)) :height height)}])])))

(defn ivories [pitches]
  (let [down (atom false)]
    (fn []
      [:ul#keyboard
       (for [k pitches]
         ^{:key (:num k)} [:li.row {:class (when (:sharp k) "sharp")
                                    :onMouseDown #(dispatch [:play-pitch k])
                                    :onMouseUp   #(dispatch [:stop-pitch k])
                                    :onMouseOut  #(dispatch [:stop-pitch k])}
                           (:name k)] )])))

(defn edit [current-part notes]
  (let [beats (:beats @current-part)
        key-list pitches]
    [:section#piano-roll
     [:section#grid
      [play-bar @current-part]
      [:div.measures
       (for [m (range beats)]
         ^{:key m} [:div.measure {:style {:width (str (/ 100.0 beats) "%")}}])]
      [:ul.notes {:onClick (fn [e]
                             (let [rect (.. e -currentTarget getBoundingClientRect)
                                   coords {:x (/ (- (.-pageX e) (.-left rect)) (.-width rect))
                                           :y (/ (- (.-pageY e) (+ (.-scrollY js/window) (.-top rect))) (.-height rect))}]
                               (dispatch [:add-note coords])))}
       (map (fn [n]
              ^{:key (:id n)}
              [:li {:style (note->style n beats)
                    :class (when (:selected? n) "selected")
                    :onClick (fn [e]
                               (.stopPropagation e)
                               (dispatch [:toggle-selection (:id n)]))}])
            @notes)]
      [:ul
       (for [k key-list]
         ^{:key (:num k)} [:li.row {:class (when (:sharp k) "sharp")}])]]
     [ivories key-list]]))
