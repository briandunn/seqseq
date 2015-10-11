(ns seqseq.components.part
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [cljs.reader]
            [seqseq.note   :refer [pitches]]
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

(def drag-x (atom {:x 0 :id nil}))

(defn- resize-width [n]
  (.log js/console (clj->js @drag-x))
  (let [{:keys [x id]} @drag-x]
    (if (= id (:id n))
      {:width "100px"}
      {})))

(defn note [n beats]
  [:li {:style (merge (note->style n beats) (resize-width n))
        :class (when (:selected? n) "selected")
        :draggable true
        :onDragStart (fn [e]
                       (let [dt (.-dataTransfer e)]
                         (set! (.-effectAllowed dt) "move")
                         (.log js/console (str {:id (:id n) :note? (not (.contains (.-classList (.-target e)) "right-handle"))}))
                         (.setData dt "text/plain" (str {:id (:id n) :note? (not (.contains (.-classList (.-target e)) "right-handle"))}))))
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
                                            img) 0 0)
                                        (set! (.-effectAllowed dt) "move")))}]])

(defn edit [current-part notes]
  (let [beats (:beats @current-part)
        dt->id (fn [e]
                 (let [data (.getData (.-dataTransfer e) "text/plain")]
                   (when (not= data "")
                     (cljs.reader/read-string data))))
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
                                (let [{:keys [id note?]} (dt->id e)]
                                  (reset! drag-x (if note? nil {:x (.-pageX e) :id id}))))
                  :onDrop (fn [e]
                            (let [{:keys [id note?]} (dt->id e)]
                              (when note?
                                (dispatch [:move-note id (event->coords e)]))))}
       (map (fn [n] ^{:key (:id n)} [note n beats]) @notes)]
      [:ul
       (for [k key-list]
         ^{:key (:num k)} [:li.row {:class (when (:sharp k) "sharp")}])]]
     [ivories key-list]]))
