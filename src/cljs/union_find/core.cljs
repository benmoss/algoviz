(ns union-find.core
  (:require [om.core :as om :include-macros true]
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

(defonce app-state (atom {:text "Hello Chestnut!"
                          :graph [1 1 2 3 4 5 6]}))

(defn handle-change [e data edit-key owner]
  (println "handle-change")
  (comment (om/transact! data edit-key (fn [_] (.. e -target -value)))))

(defn graph->dot [e graph]
  (println "handle-keypress" graph)
  (chsk-send! [:commands/quick-find->dot graph]
              1000
              (fn [resp]
                (set! js/window.foo (:dot resp))
                (let [g (.read js/graphlibDot (:dot resp))]
                  (set! (.-marginx (.graph g)) 20)
                  (set! (.-marginy (.graph g)) 20)
                  (set! (.-transition (.graph g))
                        (fn [sel] (.duration (.transition sel) 500)))
                  (.call (.select js/d3 "svg g")
                         (.render js/dagreD3)
                         g)))))

(defn main []
  (om/root
    (fn [app owner]
      (reify
        om/IRender
        (render [_]
          (dom/div
            (dom/input {:value (pr-str (:graph app))
                        :onKeyDown #(graph->dot % (:graph @app))})
            (dom/h1 (pr-str (:graph app)))
            (dom/svg {:height 600 :width 800} (dom/g))))))
    app-state
    {:target (. js/document (getElementById "app"))}))
