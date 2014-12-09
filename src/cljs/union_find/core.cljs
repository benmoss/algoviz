(ns union-find.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [taoensso.sente  :as sente :refer (cb-success?)]))

(enable-console-print!)

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
                          :graph [[1 [2 3]] [4 [5]]]}))

(defn handle-change [e data edit-key owner]
  (println "handle-change")
  (comment (om/transact! data edit-key (fn [_] (.. e -target -value)))))

(defn handle-keypress [e data]
  (println "handle-keypress")
  (when (= (.-keyCode e) 13)
    (create-message e data)))

(defn main []
  (om/root
    (fn [app owner]
      (reify
        om/IRender
        (render [_]
          (dom/div
            (dom/h1 (:text app))
            (dom/input {:value (:text app)
                        :onKeyDown #(handle-keypress % app)})))))
    app-state
    {:target (. js/document (getElementById "app"))}))
