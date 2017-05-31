(ns bottle.messaging.consumer-test
  (:require [bottle.macros :refer [with-component]]
            [bottle.messaging.consumer :as consumer]
            [clamq.activemq :as amq]
            [clamq.protocol.connection :as conn]
            [clamq.protocol.producer :as producer]
            [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]))

(def broker-path "tcp://localhost:61616")
(def config {:bottle/broker-path broker-path
             :bottle/endpoint "event-consumer-test"
             :bottle/broker-type :active-mq})

(def system (consumer/consumer config))

(with-component consumer/consumer config
  (with-open [conn (conn/activemq-connnection )]

    )
  )

(deftest constructor
  (let [consumer (consumer/consumer config)]
    (is (string? (:id consumer)))))
