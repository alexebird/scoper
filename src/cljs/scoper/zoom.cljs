;;
;; zoomable circle pack layout: http://mbostock.github.io/d3/talk/20111116/pack-hierarchy.html
;;
(ns scoper.zoom)

(def d3 window.d3)
(def margin 20)
(def view-atom (atom nil))

(defn- svg-el []
  (.select d3 "svg"))

;var svg = d3.select("svg"),
    ;margin = 20,
    ;diameter = +svg.attr("width"),
    ;g = svg.append("g").attr("transform", "translate(" + diameter / 2 + "," + diameter / 2 + ")");
(defn- px-attr-to-int [el attr]
  (int (clojure.string/replace (.style el attr) #"px" "")))

(defn- diameter [svg]
  (min (px-attr-to-int svg "height") (px-attr-to-int svg "width")))

(defn- top-level-group [svg]
  (let [dia (/ (diameter svg) 2)]
    (-> svg
        (.append "g")
        (.attr "transform" (str "translate(" dia "," dia ")")))))

;var color = d3.scaleLinear()
    ;.domain([-1, 5])
    ;.range(["hsl(152,80%,80%)", "hsl(228,30%,40%)"])
    ;.interpolate(d3.interpolateHcl);
(defn- color-fn [svg]
  (-> d3
      .scaleLinear
      (.domain #js [-1 5])
      (.range #js ["hsl(152,80%,80%)", "hsl(228,30%,40%)"])
      (.interpolate (.-interpolateHcl d3))))

;var pack = d3.pack()
    ;.size([diameter - margin, diameter - margin])
    ;.padding(2);
(defn- pack-fn [svg]
  (let [diameter (diameter svg)]
    (-> d3
        .pack
        (.size #js [(- diameter margin) (- diameter margin)])
        (.padding 2))))

;d3.json("flare.json", function(error, root) {
  ;if (error) throw error;

  ;root = d3.hierarchy(root)
      ;.sum(function(d) { return d.size; })
      ;.sort(function(a, b) { return b.value - a.value; });
(defn- mk-hierarchy [data]
  (-> d3
      (.hierarchy data)
      (.sum #(.-size %))
      (.sort #(- (.-value %2) (.-value %1)))))

  ;var focus = root,
      ;nodes = pack(root).descendants(),
      ;view;

  ;var circle = g.selectAll("circle")
    ;.data(nodes)
    ;.enter().append("circle")
      ;.attr("class", function(d) { return d.parent ? d.children ? "node" : "node node--leaf" : "node node--root"; })
      ;.style("fill", function(d) { return d.children ? color(d.depth) : null; })
      ;.on("click", function(d) { if (focus !== d) zoom(d), d3.event.stopPropagation(); });
(defn- circle [d3 g nodes focus color-fn]
  (-> g
      (.selectAll "circle")
      (.data nodes)
      .enter
      (.append "circle")
        (.attr "class" (fn [d] (if d.parent (if d.children "node" "node node--leaf") "node node--root")))
        (.style "fill" (fn [d] (if d.children (color-fn (.-depth d)) nil)))
        (.on "click" (fn [d] (if (not= focus d) (zoom-fn d focus)) (.stopPropagation (.-event d3))))))

  ;var text = g.selectAll("text")
    ;.data(nodes)
    ;.enter().append("text")
      ;.attr("class", "label")
      ;.style("fill-opacity", function(d) { return d.parent === root ? 1 : 0; })
      ;.style("display", function(d) { return d.parent === root ? "inline" : "none"; })
      ;.text(function(d) { return d.data.name; });
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

  ;var node = g.selectAll("circle,text");

  ;svg
      ;.style("background", color(-1))
      ;.on("click", function() { zoom(root); });

  ;zoomTo([root.x, root.y, root.r * 2 + margin]);

  ;function zoom(d) {
    ;var focus0 = focus; focus = d;

    ;var transition = d3.transition()
        ;.duration(d3.event.altKey ? 7500 : 750)
        ;.tween("zoom", function(d) {
          ;var i = d3.interpolateZoom(view, [focus.x, focus.y, focus.r * 2 + margin]);
          ;return function(t) { zoomTo(i(t)); };
        ;});

    ;transition.selectAll("text")
      ;.filter(function(d) { return d.parent === focus || this.style.display === "inline"; })
        ;.style("fill-opacity", function(d) { return d.parent === focus ? 1 : 0; })
        ;.on("start", function(d) { if (d.parent === focus) this.style.display = "inline"; })
        ;.on("end", function(d) { if (d.parent !== focus) this.style.display = "none"; });
  ;}

  ;function zoomTo(v) {
    ;var k = diameter / v[2]; view = v;
    ;node.attr("transform", function(d) { return "translate(" + (d.x - v[0]) * k + "," + (d.y - v[1]) * k + ")"; });
    ;circle.attr("r", function(d) { return d.r * k; });
  ;}
;});

(defn- two-r-plus-margin [node]
  (+ (* (.-r node) 2) margin))

(defn- transition [focus]
  (-> d3
      .transition
      (.duration (if (.-altKey (.-event d3)) 7500 750))
      (.tween "zoom" (fn [_]
                       (let [i (.interpolateZoom d3 @view-atom #js [(.-x focus) (.-y focus) (two-r-plus-margin focus)])]
                         ;(fn [t] (zoom-to (i t)))
                         )))

      ))

(defn zoom-fn [d orig-focus]
  (let [focus0 orig-focus
        focus d
        trans (transition focus)]
    (-> trans
        (.selectAll "text")
        (.filter (fn [d] (this-as this (or (= (.-parent d) focus) (= (.-display (.-style this)) "inline")))))
        (.style "fill-opacity" (fn [d] (if (= (.-parent d) focus) 1 0)))
        (.on "start" (fn [d] (this-as this (if (= (.-parent d) focus)    (aset this "style" "display" "inline")))))
        (.on "end"   (fn [d] (this-as this (if (not= (.-parent d) focus) (aset this "style" "display" "none")))))
        )
    ))

(defn xlate [x y]
  (str "translate(" x "," y ")"))

(defn zoom-to [dia v node circle]
  (let [k (/ dia (get v 2))]
    (reset! view-atom v)
    (-> node
        (.attr "transform" (fn [d] (xlate (- (.-x d) (get v 0)) (- (.-y d) (get v 1))))))
    (-> circle
        (.attr "r" (fn [d] (* (.-r d) k))))
    ))

(defn redraw [error data]
  (if error
    (throw error)
    (let [svg  (svg-el)
          dia  (diameter svg)
          g    (top-level-group svg)
          pack (pack-fn svg)
          root (-> data mk-hierarchy pack)
          focus root
          ;nodes (mk-dom-nodes g pack (.descendants root))
          nodes (.descendants (pack root))
          color (color-fn svg)
          circle (circle d3 g nodes focus color)
          text (text g nodes root)
          node (-> g (.selectAll "circle,text"))
          ]

      (-> svg
          (.style "background" (color -1))
          (.on "click" (fn [] (zoom-fn root focus))))

      (zoom-to
        dia
        #js [(.-x root) (.-y root) (two-r-plus-margin root)]
        node
        circle)

      )))
