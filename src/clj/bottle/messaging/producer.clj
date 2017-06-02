(ns bottle.messaging.producer)

(defprotocol Producer
  "Produces messages."
  (produce [this message] "Produce a message."))

(defmulti producer :bottle/broker-type)
