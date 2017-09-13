(ns bottle.messaging.producer.rabbitmq
  (:require [bottle.messaging.producer :refer [producer]])
  (:import [bottle.messaging.producer Producer]))

(defrecord RabbitMQProducer [broker-path queue-name]
  Producer
  (produce [this message]
    (with-open [conn (.newConnection (doto (com.rabbitmq.client.ConnectionFactory.)
                                       (.setHost broker-path)))
                chan (.createChannel conn)]
      (.queueDeclare chan queue-name true false false nil)
      (.basicPublish chan "" queue-name nil (if (string? message)
                                              (.getBytes message)
                                              message)))))

(defmethod producer :rabbit-mq
  [{:keys [:bottle.messaging/broker-path :bottle.messaging/queue-name]}]
  (map->RabbitMQProducer {:broker-path broker-path
                          :queue-name queue-name}))
