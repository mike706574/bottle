(ns bottle.client.event-producer
  (:require [bottle.message :as message]
            [bottle.api.messaging :as mq]
            [clamq.protocol.connection :as conn]
            [clamq.protocol.consumer :as consumer]
            [clamq.protocol.producer :as producer]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]))

(defprotocol EventProducer
  "Produces events."
  (produce [this content-type event] "Produce an event."))

(defrecord MQEventProducer [broker-path endpoint connector]
  EventProducer
  (produce [this content-type event]
    (with-open [conn (connector broker-path)]
      (producer/publish (conn/producer conn) endpoint (message/encode content-type event)))))

(defn event-producer
  [{:keys [event-broker-path event-endpoint] :as config}]
  (map->MQEventProducer {:broker-path event-broker-path
                         :endpoint event-endpoint
                         :connector (mq/connector config)}))
