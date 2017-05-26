(ns spiro.api.event-consumer
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [spiro.server.util :as util]
            [taoensso.timbre :as log]
            [clamq.activemq :as amq]
            [clamq.protocol.connection :as conn]
            [clamq.protocol.consumer :as consumer]
            [clamq.protocol.producer :as producer]))


(defrecord EventConsumer [id broker-uri endpoint handler conn consumer]
  component/Lifecycle
  (start [this]
    (let [conn (amq/activemq-connection broker-uri)
          consumer (conn/consumer conn {:endpoint endpoint
                                        :on-message handler
                                        :transacted true})]
      (consumer/start consumer)
      (assoc this :conn conn :consumer consumer)))
  (stop [this]
    (consumer/close consumer)
    (conn/close conn)))

(defn event-consumer
  [{:keys [event-broker-uri event-endpoint] :as config}]
  {:pre [(not (str/blank? event-broker-uri))
         (not (str/blank? event-endpoint))]}
  (component/using
   (map->EventConsumer {:id (util/uuid)
                        :broker-uri event-broker-uri
                        :endpoint event-endpoint})
   {:handler :event-handler}))
