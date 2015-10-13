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

(def drag (atom {:start-x 0 :now-x 0 :id nil}))

(defn- resize-width [n]
  (let [{:keys [id start-x now-x note?]} @drag]
    (if (and (= id (:id n)) (not note?))
      (- now-x start-x)
      0)))

(defn note [n beats]
  [:li {:style (update-in (note->style n beats) [:width] (fn [width] (str "calc(" width " + " (resize-width n) "px)")))
        :class (when (:selected? n) "selected")
        :draggable true
        :onDragStart (fn [e]
                       (set! (.. e -dataTransfer -effectAllowed) "move")
                       (reset! drag {:id (:id n)
                                     :note? (not (.contains (.-classList (.-target e)) "right-handle"))
                                     :start-x (.-pageX e)}))
        :onClick (fn [e]
                   (.stopPropagation e)
                   (dispatch [:toggle-selection (:id n)]))}
   [:div.right-handle {:draggable true
                       :onDragStart (fn [e]
                                      (let [dt (.-dataTransfer e)]
                                        (.setDragImage
                                          dt
                                          (let [img (.createElement js/document "img")]
                                            (.setAttribute img "src" "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
                                            img) 0 0)))}]])

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
                  :onDragOver (fn [e]
                                (.preventDefault e)
                                (let [{:keys [id note?]} @drag]
                                  (swap! drag assoc :now-x (.-pageX e))))
                  :onDrop (fn [e]
                            (let [{:keys [id note?]} @drag]
                              (dispatch [(if note? :move-note :resize-note) id (event->coords e)]))
                            (reset! drag {}))}
       (map (fn [n] ^{:key (:id n)} [note n beats]) @notes)]
      [:ul
       (for [k key-list]
         ^{:key (:num k)} [:li.row {:class (when (:sharp k) "sharp")}])]]
     [ivories key-list]]))
