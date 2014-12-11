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

(defonce app-state (atom {:graph [0 1 2 3 4 5 6]}))

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
  (println "graph->dot" @graph)
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

(defcomponent clickable-node [node owner]
  (render [_] (dom/label {:class "clickable-node"}
                         node (dom/input {:type "checkbox"}))))

(defcomponent base [app owner]
  (render [_]
          (dom/div
            (dom/h1 "Nodes")
            (dom/div
              (om/build-all clickable-node (:graph app)))
            (dom/button "Unify!")
            (dom/h1 "Result")
            (dom/svg {:height 600 :width 800} (dom/g))))
  (did-mount [_]
             (setup-zoom)
             (. js/window (setTimeout (partial graph->dot (:graph app)) 200))))

(defn main []
  (om/root base app-state {:target (. js/document (getElementById "app"))}))
