(ns bottle.messaging.producer.rabbitmq
  (:require [bottle.messaging.producer :refer [producer]])
  (:import [bottle.messaging.producer Producer]))

(defrecord RabbitMQProducer [broker-path endpoint connector]
  Producer
  (produce [this message]
    (with-open [conn (.newConnection (doto (com.rabbitmq.client.ConnectionFactory.)
                                       (.setHost broker-path)))
                chan (.createChannel conn)]
      (.basicPublish chan "" endpoint nil (.getBytes message)))))

(defmethod producer :rabbit-mq
  [{:keys [:bottle/broker-path :bottle/endpoint]}]
  (map->RabbitMQProducer {:broker-path broker-path
                          :endpoint endpoint}))
