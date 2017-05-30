(ns bottle.messaging.consumer-test
  (:require [bottle.messaging.consumer :as consumer]))

(def config {:bottle/event-broker-path "tcp://localhost:61616"
             :bottle/event-endpoint "event-consumer-test"
             :bottle/event-broker-type :active-mq})

(def system (consumer/consumer config))

;; TODO
