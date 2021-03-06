(ns bottle.messaging.consumer.rabbitmq
  (:require [bottle.util :as util]
            [bottle.messaging.consumer :refer [consumer]]
            [bottle.messaging.handler :as handler]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defrecord RabbitMQConsumer [id broker-path queue-name handler conn chan]
  component/Lifecycle
  (start [this]
    (log/debug (str "Starting consumer " id "."))
    (let [conn (.newConnection (doto (com.rabbitmq.client.ConnectionFactory.)
                                   (.setHost broker-path)))
          chan (.createChannel conn)
          consumer (proxy [com.rabbitmq.client.DefaultConsumer] [chan]
                     (handleDelivery [consumer-tag envelope props body]
                       (try
                         (log/trace "Processing message.")
                         (handler/handle-message handler (String. body "UTF-8"))
                         (catch Exception e
                           (log/error e (str "Exception thrown by message handler."))))))]
      (.queueDeclare chan queue-name true false false nil)
      (.basicConsume chan queue-name true consumer)
      (assoc this :conn conn :chan chan)))
  (stop [this]
    (log/debug (str "Stopping consumer " id "."))
    (.close chan)
    (.close conn)
    (dissoc this :conn :chan)))

(defmethod consumer :rabbit-mq
  [{:keys [:bottle.messaging/broker-path :bottle.messaging/queue-name] :as config}]
  (map->RabbitMQConsumer
   {:id (util/uuid)
    :broker-path broker-path
    :queue-name queue-name}))
