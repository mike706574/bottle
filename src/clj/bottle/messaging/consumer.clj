(ns bottle.messaging.consumer)

(defmulti consumer :bottle/broker-type)
