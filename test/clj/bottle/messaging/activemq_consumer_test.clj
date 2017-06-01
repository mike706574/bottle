(ns bottle.messaging.activemq-consumer-test
  (:require [bottle.macros :refer [with-system]]
            [bottle.messaging.consumer :as consumer]
            [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]
            [manifold.stream :as s])
  (:import [bottle.messaging.handler MessageHandler]))

(def broker-path "localhost")
(def endpoint "event-consumer-test")
(def config {:bottle/broker-type :active-mq
             :bottle/broker-path broker-path
             :bottle/endpoint endpoint})

(defn system [config]
  (let [messages (s/stream)]
    {:messages messages
     :consumer (consumer/consumer config)
     :message-handler (reify MessageHandler
                        (handle-message [_ message-bytes]
                          (let [message (String. message-bytes "UTF-8")]
                            (println "Message!!!" message)
                            (s/put! messages message))))}))

(defn send-message
  [message]
  (def conn (.newConnection (doto (com.rabbitmq.client.ConnectionFactory.)
                              (.setHost broker-path))))
  (def chan (.createChannel conn))
  (.basicPublish chan "" endpoint nil (.getBytes message))
  (.close chan)
  (.close conn))

(deftest messages
  (with-system (system config)
    (let [messages (:messages system)]
      (send-message "One!")
      (send-message "Two!")
      (is (= "One!" @(s/try-take! messages :drained-1 100 :timeout-1)))
      (is (= "Two!" @(s/try-take! messages :drained-2 100 :timeout-2))))))

(deftest constructor
  (let [consumer (consumer/consumer config)]
    (is (string? (:id consumer)))))
