(ns union-find.event-handlers)

(defn- logf [fmt & xs] (println (apply format fmt xs)))

(defmulti event-msg-handler :id) ; Dispatch on event-id

(defmethod event-msg-handler :commands/graph->dot
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (?reply-fn {:cool "lets go!!"}))

(defmethod event-msg-handler :default ; Fallback
    [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
    (let [session (:session ring-req)
          uid     (:uid     session)]
      (logf "Unhandled event: %s" event)
      (when ?reply-fn
        (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))
