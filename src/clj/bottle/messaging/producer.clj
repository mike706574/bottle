(ns bottle.messaging.producer
  (:require [bottle.message :as message]
            [bottle.messaging.connector :as connector]
            [clamq.protocol.connection :as conn]
            [clamq.protocol.producer :as producer]
            [taoensso.timbre :as log]))

(defprotocol Producer
  "Produces messages."
  (produce [this content-type message] "Produce a message."))

(defrecord ClamProducer [broker-path endpoint connector]
  Producer
  (produce [this content-type event]
    (with-open [conn (connector broker-path)]
      (producer/publish (conn/producer conn) endpoint (message/encode content-type event)))))

(defn producer
  [{:keys [event-broker-path event-endpoint] :as config}]
  (map->ClamProducer {:broker-path event-broker-path
                      :endpoint event-endpoint
                      :connector (connector/connector config)}))
