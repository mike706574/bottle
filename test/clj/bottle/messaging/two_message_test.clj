(ns bottle.messaging.two-message-test
  (:require [bottle.macros :refer [with-system]]
            [bottle.messaging.consumer :as consumer]
            [bottle.messaging.producer :as producer]

            [bottle.messaging.consumer.activemq]
            [bottle.messaging.consumer.rabbitmq]
            [bottle.messaging.consumer.stream]

            [bottle.messaging.producer.activemq]
            [bottle.messaging.producer.rabbitmq]
            [bottle.messaging.producer.stream]

            [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [manifold.stream :as stream])
  (:import [bottle.messaging.handler MessageHandler]))

(def queue-name "two-message-test")

(defn system [config deps]
  (let [messages (stream/stream)]
    (merge deps
           {:messages messages
            :consumer (component/using
                       (consumer/consumer config)
                       [:handler])
            :producer (producer/producer config)
            :handler (reify MessageHandler
                       (handle-message [_ message]
                         (stream/put! messages message)))})))

(defn send-two-messages [config deps]
  (with-system (system config deps)
    (let [{:keys [messages producer]} system]
      (producer/produce producer "One!")
      (producer/produce producer "Two!")
      (is (= "One!" @(stream/try-take! messages :drained-1 500 :timeout-1)))
      (is (= "Two!" @(stream/try-take! messages :drained-2 500 :timeout-2))))))

(deftest activemq
  (testing "Sending and receiving 2 messages with ActiveMQ."
    (send-two-messages {:bottle.messaging/broker-type :active-mq
                        :bottle.messaging/broker-path "tcp://localhost:62626"
                        :bottle.messaging/queue-name queue-name}
                       {})))

(deftest rabbitmq
  (testing "Sending and receiving 2 messages with RabbitMQ."
    (send-two-messages {:bottle.messaging/broker-type :rabbit-mq
                        :bottle.messaging/broker-path "localhost"
                        :bottle.messaging/queue-name queue-name}
                       {})))

(deftest stream
  (testing "Sending and receiving 2 messages with a Manifold stream."
    (send-two-messages {:bottle.messaging/broker-type :stream
                        :bottle.messaging/stream-id :test-stream}
                       {:test-stream (stream/stream)})))
