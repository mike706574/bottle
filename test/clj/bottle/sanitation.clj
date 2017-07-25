(ns bottle.sanitation)

(defn purge [event]
  (if (map? event)
    (dissoc event :bottle/id :bottle/time)
    event))

(defn purge-message [message]
  (if (= :timeout message)
    message
    (update message :bottle/event purge)))

(defn purge-all
  [body]
  (into #{} (map purge body)))
