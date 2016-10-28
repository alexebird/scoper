;;
;; zoomable circle pack layout: http://mbostock.github.io/d3/talk/20111116/pack-hierarchy.html
;;
(ns scoper.zoom)

(def d3 window.d3)
(def margin 20)
(def view-atom (atom nil))
(def circle-behavior (atom nil))
(def dia (atom nil))

(defn- svg-el []
  (.select d3 "svg"))

(defn- px-attr-to-int [el attr]
  (int (clojure.string/replace (.style el attr) #"px" "")))

(defn- diameter [svg]
  (min (px-attr-to-int svg "height") (px-attr-to-int svg "width")))

(defn- top-level-group [svg dia]
  (-> svg
      (.append "g")
      (.attr "transform" (str "translate(" (/ dia 2) "," (/ dia 2) ")"))))

(defn- color-fn [svg]
  (-> d3
      .scaleLinear
      (.domain #js [-1 5])
      (.range #js ["hsl(152,80%,80%)", "hsl(228,30%,40%)"])
      (.interpolate (.-interpolateHcl d3))))

(defn- pack-fn [svg dia]
  (-> d3
      .pack
      (.size #js [(- dia margin) (- dia margin)])
      (.padding 2)))

(defn- mk-hierarchy [data]
  (-> d3
      (.hierarchy data)
      (.sum #(.-size %))
      (.sort #(- (.-value %2) (.-value %1)))))

(defn- circle [d3 g nodes focus color-fn]
  (-> g
      (.selectAll "circle")
      (.data nodes)
      .enter
      (.append "circle")
        (.attr "class" (fn [d] (if d.parent (if d.children "node" "node node--leaf") "node node--root")))
        (.style "fill" (fn [d] (if d.children (color-fn (.-depth d)) nil)))
        (.on "click" (fn [d] (if (not= focus d) (zoom-fn d focus g)) (.stopPropagation (.-event d3))))))

(defn- text [g nodes root]
  (-> g
      (.selectAll "text")
      (.data nodes)
      .enter
      (.append "text")
        (.attr "class" "label")
        (.style "fill-opacity" (fn [d] (if (= d.parent root) 1 0)))
        (.style "display" (fn [d] (if (= d.parent root) "inline" "none")))
        (.text (fn [d] (.-name (.-data d))))))

(defn- two-r-plus-margin [node]
  (+ (* (.-r node) 2) margin))

(defn- transition [focus callback]
  (-> d3
      .transition
      (.duration (if (.-altKey (.-event d3)) 7500 750))
      (.tween "zoom" (fn [_]
                       (let [interpolation-fn (.interpolateZoom d3 @view-atom #js [(.-x focus) (.-y focus) (two-r-plus-margin focus)])]
                         (fn [t] (callback (interpolation-fn t))))))))

(defn- zoom-fn [d orig-focus g]
  (let [focus0 orig-focus
        focus d
        trans (transition focus (fn [target] (zoom-to g target)))]
    (-> trans
        (.selectAll "text")
        (.filter (fn [d] (this-as this (or (= (.-parent d) focus) (= (.-display (.-style this)) "inline")))))
        (.style "fill-opacity" (fn [d] (if (= (.-parent d) focus) 1 0)))
        (.on "start" (fn [d] (this-as this (if (= (.-parent d) focus)    (aset this "style" "display" "inline")))))
        (.on "end"   (fn [d] (this-as this (if (not= (.-parent d) focus) (aset this "style" "display" "none")))))
        )
    ))

(defn- xlate [x y]
  (str "translate(" x "," y ")"))

(defn- zoom-to [g view]
  (let [node (-> g (.selectAll "circle,text"))
        k (/ @dia (get view 2))]
    (reset! view-atom view)
    (-> node
        (.attr "transform" (fn [d] (xlate (- (.-x d) (get view 0)) (- (.-y d) (get view 1))))))
    (-> @circle-behavior
        (.attr "r" (fn [d] (* (.-r d) k))))
    ))

(defn redraw [error data]
  (if error
    (throw error)
    (let [svg   (svg-el)
          _dia  (diameter svg)
          g     (top-level-group svg _dia)
          pack  (pack-fn svg _dia)
          root  (-> data mk-hierarchy pack)
          focus root
          nodes (.descendants (pack root))
          color (color-fn svg)
          text  (text g nodes root)]

      (reset! dia _dia)
      (reset! circle-behavior (circle d3 g nodes focus color))

      (-> svg
          (.style "background" (color -1))
          (.on "click" (fn [] (zoom-fn root focus g))))

      (zoom-to
        g
        #js [(.-x root) (.-y root) (two-r-plus-margin root)]
        @circle-behavior)

      )))
