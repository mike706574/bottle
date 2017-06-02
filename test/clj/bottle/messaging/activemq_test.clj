(ns bottle.messaging.activemq-test
  (:require [bottle.macros :refer [with-system]]
            [bottle.messaging.consumer :as consumer]
            [bottle.messaging.producer :as producer]
            [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]
            [manifold.stream :as s])
  (:import [bottle.messaging.handler MessageHandler]))

(def broker-path "tcp://localhost:61616")
(def queue-name "event-consumer-test")
(def config {:bottle/broker-type :active-mq
             :bottle/broker-path broker-path
             :bottle/queue-name queue-name})

(defn system [config]
  (let [messages (s/stream)]
    {:messages messages
     :consumer (consumer/consumer config)
     :message-handler (reify MessageHandler
                        (handle-message [_ message]
                          (s/put! messages message)))}))

(defn send-message
  [message]
  (producer/produce (producer/producer config) message))

(deftest messages
  (with-system (system config)
    (let [messages (:messages system)]
      (send-message "One!")
      (send-message "Two!")
      (is (= "One!" @(s/try-take! messages :drained-1 500 :timeout-1)))
      (is (= "Two!" @(s/try-take! messages :drained-2 500 :timeout-2))))))

(deftest constructor
  (let [consumer (consumer/consumer config)]
    (is (string? (:id consumer)))))
