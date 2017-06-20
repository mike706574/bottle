(ns bottle.messaging.consumer.activemq
  (:require [bottle.util :as util]
            [bottle.messaging.consumer :refer [consumer]]
            [bottle.messaging.handler :as handler]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defrecord ActiveMQConsumer [id broker-path queue-name handler conn session consumer]
  component/Lifecycle
  (start [this]
    (log/debug (str "Starting consumer " id "."))
    (let [factory (org.apache.activemq.ActiveMQConnectionFactory. broker-path)
          conn (.createConnection factory)
          session (.createSession conn false javax.jms.Session/AUTO_ACKNOWLEDGE)
          dest (.createQueue session queue-name)
          listener (proxy [javax.jms.MessageListener] []
                     (onMessage [message-object]
                       (try
                         (log/trace "Processing message.")
                         (handler/handle-message handler (.getText message-object))
                         (catch Exception e
                             (log/error e (str "Exception thrown by message handler."))))))
          consumer (.createConsumer session dest)]
      (.setMessageListener consumer listener)
      (.start conn)
      (assoc this :conn conn :session session :consumer consumer)))
  (stop [this]
    (log/debug (str "Stopping consumer " id "."))
    (.close consumer)
    (.close session)
    (.close conn)
    (dissoc this :cnn :session :consumer)))

(defmethod consumer :active-mq
  [{:keys [:bottle.messaging/broker-path :bottle.messaging/queue-name] :as config}]
  (map->ActiveMQConsumer
   {:id (util/uuid)
    :broker-path broker-path
    :queue-name queue-name}))
