(ns union-find.core
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [taoensso.sente  :as sente :refer (cb-success?)]))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" ; Note the same path as before
       {:type :auto ; e/o #{:auto :ajax :ws}
       })]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(enable-console-print!)

(defn gen-graph [length]
  (mapv (fn [i] {:label i :selected false})
        (range length)))

(defonce app-state (atom {:graph (gen-graph 10)}))

(defn scale-svg [d3-graph]
  (let [svg (.select js/d3 "svg")
        svg-group (.select js/d3 "svg g")
        x-center-offset (/ (- (.attr svg "width") (.-width (.graph d3-graph))) 2)]
    (.attr svg-group "transform" (str "translate(" x-center-offset ", 20)"))
    (.attr svg "height" (+ 40 (.-height (.graph d3-graph))))))

(defn draw-dot [resp]
  (let [g (.read js/graphlibDot (:dot resp))]
    (set! (.-marginx (.graph g)) 20)
    (set! (.-marginy (.graph g)) 20)
    (set! (.-transition (.graph g))
          (fn [sel] (.duration (.transition sel) 500)))
    (.call (.select js/d3 "svg g")
           (.render js/dagreD3)
           g)))

(defn graph->dot [graph]
  (chsk-send! [:commands/quick-find->dot @graph] 1000 draw-dot))

(defn setup-zoom []
  (let [svg (. js/d3 select "svg")
        inner (. js/d3 select "svg g")
        on-zoom (fn [] (.attr inner
                              "transform",
                              (str "translate(" (.-translate (.-event js/d3)) ")"
                                   "scale(" (.-scale (.-event js/d3)) ")")))
        zoom (.on (.zoom (.-behavior js/d3)) "zoom" on-zoom)]
    (.call svg zoom)))

(defn unify [graph]
  (let [g @graph
        [p q] (filter :selected g)]
    (let [unify-node #(if (= (:label p) (:label %))
                        (assoc % :label (:label q))
                        %)
          unselect #(assoc % :selected false)
          new-graph (mapv (comp unify-node unselect) g)]
      (om/update! graph new-graph))))

(defcomponent clickable-node [node owner]
  (render [_]
          (dom/label {:class "clickable-node"}
                     (:label node)
                     (dom/input {:type "checkbox"
                                 :checked (:selected node)
                                 :onClick #(om/update! node :selected (.-checked (.-target %)))}))))

(defcomponent nodes [graph owner]
  (render [_] (dom/div
                (dom/h1 "Nodes")
                (dom/div (om/build-all clickable-node graph))
                (dom/button {:onClick #(unify graph)} "Unify!")))
  (did-mount [_]
             (setup-zoom)
             (. js/window (setTimeout (partial graph->dot graph) 200)))
  (will-update [_ _ _]
              (. js/window (setTimeout (partial graph->dot graph) 200))))

(defcomponent base [app owner]
  (render [_]
          (dom/div
            (om/build nodes (:graph app))
            (dom/h1 "Result")
            (dom/svg {:height 600 :width 800} (dom/g))))
  )

(defn main []
  (om/root base app-state {:target (. js/document (getElementById "app"))}))
