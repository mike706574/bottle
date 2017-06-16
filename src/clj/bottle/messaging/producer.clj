(ns bottle.messaging.producer)

(defprotocol Producer
  "Produces messages."
  (produce [this message] "Produce a message."))

(defmulti producer :bottle/broker-type)

(defmethod producer :default
  [{broker-type :bottle/broker-type :as config}]
  (throw (ex-info (str "Invalid broker type: \"" broker-type "\"") (or config {}))))
