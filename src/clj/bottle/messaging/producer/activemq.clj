(ns bottle.messaging.producer.activemq
  (:require [bottle.messaging.producer :refer [producer]])
  (:import [bottle.messaging.producer Producer]))

(defrecord ActiveMQProducer [broker-path queue-name connector]
  Producer
  (produce [this message]
    (with-open [conn (.createConnection (org.apache.activemq.ActiveMQConnectionFactory. broker-path))
                session (.createSession conn false javax.jms.Session/AUTO_ACKNOWLEDGE)]
      (let [producer (.createProducer session (.createQueue session queue-name))
            message (.createTextMessage session message)]
        (.setDeliveryMode producer (javax.jms.DeliveryMode/NON_PERSISTENT))
        (.send producer message)))))

(defmethod producer :active-mq
  [{:keys [:bottle.messaging/broker-path :bottle.messaging/queue-name]}]
  (map->ActiveMQProducer {:broker-path broker-path
                          :queue-name queue-name}))
