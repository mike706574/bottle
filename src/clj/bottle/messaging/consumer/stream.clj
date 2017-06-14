(ns bottle.messaging.consumer.stream
  (:require [bottle.util :as util]
            [bottle.messaging.consumer :refer [consumer]]
            [bottle.messaging.handler :as handler]
            [com.stuartsierra.component :as component]
            [manifold.stream :as stream]
            [taoensso.timbre :as log]))

(defrecord StreamConsumer [id handler stream]
  component/Lifecycle
  (start [this]
    (log/debug (str "Starting consumer " id "."))
    (stream/consume
     (fn [message]
       (try
         (log/trace "Processing message.")
         (handler/handle-message handler message)
         (catch Exception e
           (log/error e (str "Exception thrown by message handler.")))))
     stream)
    this)
  (stop [this]
    (log/debug (str "Stopping consumer " id "."))
    (stream/close! stream)
    (dissoc this :cnn :session :consumer)))

(defmethod consumer :stream
  [config]
  (component/using (map->StreamConsumer {:id (util/uuid)}) [:consumer-stream]))
