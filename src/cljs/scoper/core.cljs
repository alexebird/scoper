;
; zoomable cirle pack layout: http://mbostock.github.io/d3/talk/20111116/pack-hierarchy.html
;
(ns scoper.core
    (:require [reagent.core :as reagent :refer [atom cursor]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

(defonce app-state (reagent/atom {:svg {:width 960 :height 760}}))

(def d3 window.d3)
(def num-format (.format d3 ",d"))

(defn- svg-el []
  (.select d3 "svg"))

(defn- px-attr-to-int [el attr]
  (int (clojure.string/replace (.style el attr) #"px" "")))

(defn- diameter [svg]
  (min (px-attr-to-int svg "height") (px-attr-to-int svg "width")))

(defn- top-level-group [svg]
  (-> svg (.append "g") (.attr "transform" "translate(2,2)")))

(defn- pack-fn [svg]
  (let [diameter (diameter svg)]
    (-> d3 .pack (.size #js [(- diameter 4) (- diameter 4)]))))

(defn- mk-hierarchy [data]
  (-> d3
      (.hierarchy data)
      (.sum #(.-size %))
      (.sort #(- (.-value %2) (.-value %1)))))

(defn- mk-dom-nodes [g pack root]
  (-> g
      (.selectAll ".node")
      (.data root)
      (.enter)
      (.append "g")
      (.attr "class" #(if (.-children %) "node" "leaf node"))
      (.attr "transform"
             #(str "translate(" (.-x %) "," (.-y %) ")"))))

(defn- decorate-nodes [nodes]
  (-> nodes
      (.append "title")
      (.text #(str (-> % .-data .-name) "\n" (num-format (.-value %)))))
  (-> nodes
      (.append "circle")
      (.attr "r" #(.-r %)))
  (-> nodes
      (.filter #(not (.-children %)))
      (.append "text")
      (.attr "dy" "0.3em")
      (.text #(str (-> % .-data .-name (.substring 0 (/ (.-r %) 3)))))))

(defn json-fn [error data]
  (if error
    (throw error)
    (let [svg  (svg-el)
          g    (top-level-group svg)
          pack (pack-fn svg)
          root (-> data mk-hierarchy pack)
          nodes (mk-dom-nodes g pack (.descendants root))]
      (decorate-nodes nodes))))

(defn draw []
  (.json d3 "flare.json" json-fn))

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
    [:button {:on-click #(draw)} "refresh"]
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
  (reagent/render [current-page] (.getElementById js/document "app"))
  (draw))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
