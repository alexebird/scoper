(ns scoper.core
    (:require [reagent.core :as reagent :refer [atom cursor]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              ))

(defonce app-state (reagent/atom {:svg {:width 960 :height 760}}))

(def d3 window.d3)

;var svg = d3.select("svg"),
    ;diameter = +svg.attr("width"),
    ;g = svg.append("g").attr("transform", "translate(2,2)"),
    ;format = d3.format(",d");

;var pack = d3.pack()
    ;.size([diameter - 4, diameter - 4]);

;d3.json("flare.json", function(error, root) {
  ;if (error) throw error;

  ;root = d3.hierarchy(root)
      ;.sum(function(d) { return d.size; })
      ;.sort(function(a, b) { return b.value - a.value; });

  ;var node = g.selectAll(".node")
    ;.data(pack(root).descendants())
    ;.enter().append("g")
      ;.attr("class", function(d) { return d.children ? "node" : "leaf node"; })
      ;.attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });

  ;node.append("title")
      ;.text(function(d) { return d.data.name + "\n" + format(d.value); });

  ;node.append("circle")
      ;.attr("r", function(d) { return d.r; });

  ;node.filter(function(d) { return !d.children; }).append("text")
      ;.attr("dy", "0.3em")
      ;.text(function(d) { return d.data.name.substring(0, d.r / 3); });
;});

(defn json-fn [g pack format]
  (fn [error root]
    (if error (throw error))
    (let [
          root (-> d3
                   (.hierarchy root)
                   (.sum #(.-size %))
                   (.sort #(- (.-value %2) (.-value %1))))
          node (-> g
                   (.selectAll ".node")
                   (.data (.descendants (pack root)))
                   (.enter)
                   (.append "g")
                   (.attr "class" #(if (.-children %) "node" "leaf node"))
                   (.attr "transform" #(str "translate(" (.-x %) "," (.-y %) ")"))
                   )
          ]

      (-> node
          (.append "title")
          (.text #(str (-> % .-data .-name) "\n" (format (.-value %))))
          )

      (-> node
          (.append "circle")
          (.attr "r" #(.-r %))
          )

      (-> node
          (.filter #(not (.-children %)))
          (.append "text")
          (.attr "dy" "0.3em")
          (.text #(str (-> % .-data .-name (.substring 0 (/ (.-r %) 3)))))
          )

      )))

(defn draw []
  (let [
        svg      (.select d3 "svg")
        diameter (.style svg "width")
        g        (-> svg (.append "g") (.attr "transform" "translate(2,2)"))
        format   (.format d3 ",d")
        pack     (-> d3 (.pack) (.size [(- diameter 4) (- diameter 4)]))
        ]
    (js/console.log "foo")
    (.json d3 "json.json" (json-fn g pack format))
    nil))

;; -------------------------
;; Views

(defn d3-thing []
  (let [data (reagent/cursor app-state [:svg])]
    [:svg#svgland]))

(defn d3-thing-with-callbacks []
  (with-meta d3-thing
             {:should-component-update
              (fn [this old-argv new-argv]
                false)}))

(defn home-page []
  [d3-thing])

;(defn home-page []
  ;[:div [:h2 "Welcome to scoper"]
   ;[:div [:a {:href "/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About scoper"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div#react-root
   [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root)
  (draw))
