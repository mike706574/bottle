(ns bottle.messaging.producer
  (:require [bottle.message :as message]
            [bottle.messaging.connector :as connector]
            [taoensso.timbre :as log]))

(defprotocol Producer
  "Produces messages."
  (produce [this content-type message] "Produce a message."))

(defrecord ClamProducer [broker-path endpoint]
  Producer
  (produce [this content-type event]))

(defn producer
  [{:keys [event-broker-path event-endpoint] :as config}]
  (map->ClamProducer {:broker-path event-broker-path
                      :endpoint event-endpoint}))
