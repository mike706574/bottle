(ns bottle.messaging.consumer-amq-test
  (:require [bottle.macros :refer [with-system]]
            [bottle.messaging.consumer :as consumer]
            [clamq.activemq :as amq]
            [clamq.protocol.connection :as conn]
            [clamq.protocol.producer :as producer]
            [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]
            [manifold.stream :as s])
  (:import [bottle.messaging.handler MessageHandler]))

(def broker-path "tcp://localhost:61616")
(def endpoint "event-consumer-test")
(def config {:bottle/broker-type :active-mq
             :bottle/broker-path broker-path
             :bottle/endpoint endpoint})

(defn system [config]
  (let [messages (s/stream)]
    {:messages messages
     :consumer (consumer/consumer config)
     :message-handler (reify MessageHandler
                        (handle-message [_ message]
                          (s/put! messages message)))}))

(deftest messages
  (with-system (system config)
    (with-open [conn (amq/activemq-connection broker-path)]
      (producer/publish (conn/producer conn) endpoint "One!")
      (producer/publish (conn/producer conn) endpoint "Two!"))
    (let [messages (:messages system)]
      (is (= "One!" @(s/try-take! messages :drained-1 100 :timeout-1)))
      (is (= "Two!" @(s/try-take! messages :drained-2 100 :timeout-2))))))

(deftest constructor
  (let [consumer (consumer/consumer config)]
    (is (string? (:id consumer)))))
