(ns bottle.messaging.producer.rabbitmq
  (:require [bottle.messaging.producer :refer [producer]])
  (:import [bottle.messaging.producer Producer]))

(defrecord RabbitMQProducer [broker-path queue-name connector]
  Producer
  (produce [this message]
    (with-open [conn (.newConnection (doto (com.rabbitmq.client.ConnectionFactory.)
                                       (.setHost broker-path)))
                chan (.createChannel conn)]
      (.basicPublish chan "" queue-name nil (.getBytes message)))))

(defmethod producer :rabbit-mq
  [{:keys [:bottle/broker-path :bottle/queue-name]}]
  (map->RabbitMQProducer {:broker-path broker-path
                          :queue-name queue-name}))
