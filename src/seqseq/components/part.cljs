(ns seqseq.components.part (:require [seqseq.note   :refer [pitches]]
                                     [re-frame.core :refer [subscribe]]))

(defn- f->% [f]
  (str (* 100 f) "%"))

(defn- note->style [note beats]
  (let [{:keys [tick beat pitch duration]} note
        pitches (count pitches)
        part-ticks (* 96 beats) ]
    {:left  (f->% (/ (+ tick (* 96 beat)) part-ticks))
     :top   (f->% (/ (- pitches pitch 1) pitches))
     :width (f->% (/ duration part-ticks))}))

(defn summary [part]
  (let [notes  (deref (subscribe [:notes part]))
        height (f->% (/ 1 (count pitches))) ]
    [:ul.notes
     (for [n notes]
       ^{:key (:id n)} [:li {:style (assoc (note->style n (:beats part)) :height height)}])]))

(defn edit [current-part notes play-bar {:keys [on-note-click on-note-add]}]
  (let [beats (:beats @current-part)
        key-list pitches]
    [:section#piano-roll
     [:section#grid
      [play-bar current-part]
      [:div.measures
       (for [m (range beats)]
         ^{:key m} [:div.measure {:style {:width (str (/ 100.0 beats) "%")}}])]
      [:ul.notes {:onClick (fn [e]
                             (on-note-add
                               (let [rect (.. e -target getBoundingClientRect)]
                                 {:x (/ (- (.-screenX e) (.-left rect)) (.-width rect))
                                  :y (/ (- (.-pageY e) (+ (.-scrollY js/window) (.-top rect))) (.-height rect))})))}
       (map (fn [n]
              ^{:key (:id n)}
              [:li {:style (note->style n beats)
                    :class (when (:selected? n) "selected")
                    :onClick (fn [e]
                               (.stopPropagation e)
                               (on-note-click (:id n)))}])
            @notes)]
      [:ul
       (for [k key-list]
         ^{:key (:num k)} [:li.row {:class (when (:sharp k) "sharp")}])]]
     [:ul#keyboard (for [k key-list]
                     ^{:key (:num k)} [:li.row {:class (when (:sharp k) "sharp")} (:name k)] )]]))
