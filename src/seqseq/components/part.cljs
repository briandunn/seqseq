(ns seqseq.components.part
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [seqseq.note   :refer [pitches]]
            [reagent.core  :refer [next-tick atom]]
            [re-frame.core :refer [subscribe dispatch]]))

(defn- f->% [f]
  (str (* 100 f) "%"))

(defn note->style [note beats]
  (let [{:keys [tick beat pitch duration]} note
        pitches (count pitches)
        part-ticks (* 96 beats) ]
    {:left  (f->% (/ (+ tick (* 96 beat)) part-ticks))
     :top   (f->% (/ (- pitches pitch 1) pitches))
     :width (f->% (/ duration part-ticks))}))

(defn play-bar [part]
  (let [position (subscribe [:play-head-position (:id part)])
        play-state (subscribe [:transport])
        playing? (reaction (= :play @play-state))
        counter (atom 0)
        transition? (reaction (and (not= 0 (mod @counter 3)) @playing?))]
    (fn [part]
      (let [[left s] @position]
        (next-tick #(swap! counter inc))
        [:div.play-bar {:style {:left (f->% left)
                                :transition-duration (str s "s")}
                        :class (if @transition? "go" "stop")}]))))

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

(defn- event->coords [e]
  (let [rect (.. e -currentTarget getBoundingClientRect)]
    {:x (/ (- (.-pageX e) (.-left rect)) (.-width rect))
     :y (/ (- (.-pageY e) (+ (.-scrollY js/window) (.-top rect))) (.-height rect))}))

(defn note [n beats]
  ^{:key (:id n)} [:li {:style (note->style n beats)
                        :class (when (:selected? n) "selected")
                        :draggable true
                        :onDragStart (fn [e] (let [dt (.-dataTransfer e)]
                                               (set! (.-effectAllowed dt) "move")
                                               (.setData dt "text/plain" (str (:id n)))))
                        :onClick (fn [e]
                                   (.stopPropagation e)
                                   (dispatch [:toggle-selection (:id n)]))}])

(defn edit [current-part notes]
  (let [beats (:beats @current-part)
        key-list pitches]
    [:section#piano-roll
     [:section#grid
      [play-bar @current-part]
      [:div.measures
       (for [m (range beats)]
         ^{:key m} [:div.measure {:style {:width (str (/ 100.0 beats) "%")}}])]
      [:ul.notes {:onClick #(dispatch [:add-note (event->coords %)])
                  :onDragOver #(.preventDefault %)
                  :onDrop #(dispatch [:move-note (int (.getData (.-dataTransfer %) "text/plain")) (event->coords %)]) }
       (map (fn [n] [note n beats]) @notes)]
      [:ul
       (for [k key-list]
         ^{:key (:num k)} [:li.row {:class (when (:sharp k) "sharp")}])]]
     [ivories key-list]]))
