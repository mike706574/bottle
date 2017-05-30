(ns bottle.api.event-consumer
  (:require [bottle.util :as util]
            [bottle.api.event-handler :as event-handler]
            [bottle.api.messaging :as mq]
            [clamq.protocol.connection :as conn]
            [clamq.protocol.consumer :as consumer]
            [clamq.protocol.producer :as producer]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defrecord EventConsumer [id broker-path endpoint handler connector conn consumer]
  component/Lifecycle
  (start [this]
    (log/info (str "Starting event consumer " id "..."))
    (let [conn (connector broker-path)
          consumer (conn/consumer
                    conn
                    {:endpoint endpoint
                     :on-message (partial event-handler/handle-event handler)
                     :transacted false})]
      (consumer/start consumer)
      (log/info (str "Finished starting."))
      (assoc this :conn conn :consumer consumer)))
  (stop [this]
    (log/info (str "Stopping event consumer " id "..."))
    (consumer/close consumer)
    (conn/close conn)
    (dissoc this :conn :consumer)))

(defn event-consumer
  [{:keys [:bottle/event-broker-path :bottle/event-endpoint] :as config}]
  (component/using
   (map->EventConsumer {:id (util/uuid)
                        :broker-path event-broker-path
                        :endpoint event-endpoint
                        :connector (mq/connector config) })
   {:handler :event-handler}))
