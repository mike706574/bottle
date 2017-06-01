(ns bottle.messaging.consumer
  (:require [bottle.util :as util]
            [bottle.messaging.connector :as connector]
            [bottle.messaging.handler :as handler]
            [clamq.protocol.connection :as conn]
            [clamq.protocol.consumer :as consumer]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defrecord Consumer [id broker-path endpoint handler connector conn consumer]
  component/Lifecycle
  (start [this]
    (log/info (str "Starting consumer " id "..."))
    (let [conn (connector broker-path)
          consumer (conn/consumer
                    conn
                    {:endpoint endpoint
                     :on-message (partial handler/handle-message handler)
                     :transacted false})]
      (consumer/start consumer)
      (log/info (str "Finished starting."))
      (assoc this :conn conn :consumer consumer)))
  (stop [this]
    (log/info (str "Stopping consumer " id "..."))
    (consumer/close consumer)
    (conn/close conn)
    (dissoc this :conn :consumer)))

(defn consumer
  [{:keys [:bottle/broker-path :bottle/endpoint] :as config}]
  (component/using
   (map->Consumer {:id (util/uuid)
                   :broker-path broker-path
                   :endpoint endpoint
                   :connector (connector/connector config) })
   {:handler :message-handler}))
