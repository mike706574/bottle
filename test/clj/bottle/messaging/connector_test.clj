(ns bottle.messaging.connector-test
  (:require [clamq.activemq :as amq]
            [clamq.rabbitmq :as rmq]
            [bottle.messaging.connector :as connector]
            [clojure.test :refer [deftest is]]))

(deftest connector
  (is (= amq/activemq-connection
         (connector/connector {:bottle/event-broker-type :active-mq})))
  (is (= rmq/rabbitmq-connection
         (connector/connector {:bottle/event-broker-type :rabbit-mq}))))
