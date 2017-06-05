(ns bottle.messaging.two-message-test
  (:require [bottle.macros :refer [with-system]]
            [bottle.messaging.consumer :as consumer]
            [bottle.messaging.producer :as producer]
            [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [manifold.stream :as s])
  (:import [bottle.messaging.handler MessageHandler]))

(def queue-name "two-message-test")

(defn system [config]
  (let [messages (s/stream)]
    {:messages messages
     :consumer (component/using
                   (consumer/consumer config)
                 [:handler])
     :producer (producer/producer config)
     :handler (reify MessageHandler
                (handle-message [_ message]
                  (s/put! messages message)))}))

(defn send-two-messages [config]
  (with-system (system config)
    (let [{:keys [messages producer]} system]
      (producer/produce producer "One!")
      (producer/produce producer "Two!")
      (is (= "One!" @(s/try-take! messages :drained-1 500 :timeout-1)))
      (is (= "Two!" @(s/try-take! messages :drained-2 500 :timeout-2))))))

(deftest activemq
  (testing "Sending and receiving 2 messages with ActiveMQ."
    (send-two-messages {:bottle/broker-type :active-mq
                        :bottle/broker-path "tcp://localhost:61616"
                        :bottle/queue-name queue-name})))

(deftest rabbitmq
  (testing "Sending and receiving 2 messages with RabbitMQ."
    (send-two-messages {:bottle/broker-type :rabbitmq-mq
                        :bottle/broker-path "localhost"
                        :bottle/queue-name queue-name})))
