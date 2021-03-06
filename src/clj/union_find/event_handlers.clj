(ns union-find.event-handlers
  (:require [rhizome.viz :refer [view-graph]]
            [rhizome.dot :refer [graph->dot]]
            [taoensso.timbre :as log]))

(defn- logf [fmt & xs] (log/info (apply format fmt xs)))

(defn child? [parent i n]
  (when (and (not= i n)
             (= parent n))
    i))

(defn quick-find->dot [v]
  (let [labels (mapv :label v)]
    (graph->dot (range 0 (count v))
                (fn [n] (keep-indexed (partial child? n) labels))
                :node->descriptor (fn [n] {:label n}))))

(defmulti event-msg-handler :id) ; Dispatch on event-id

(defmethod event-msg-handler :commands/quick-find->dot
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (?reply-fn {:dot (quick-find->dot ?data)}))

(defmethod event-msg-handler :default ; Fallback
    [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
    (let [session (:session ring-req)
          uid     (:uid     session)]
      (logf "Unhandled event: %s" event)
      (when ?reply-fn
        (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(comment

  (rhizome.viz/view-image (rhizome.viz/dot->image (quick-find->dot [{:label 1} {:label 1} {:label 1} {:label 2} {:label 3} {:label 4}])))

  )
