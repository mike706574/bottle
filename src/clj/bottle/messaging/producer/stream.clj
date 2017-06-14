(ns bottle.messaging.producer.stream
  (:require [bottle.messaging.producer :refer [producer]]
            [com.stuartsierra.component :as component]
            [manifold.stream :as stream])
  (:import [bottle.messaging.producer Producer]))

(defrecord StreamProducer [stream]
  Producer
  (produce [this message]
    (stream/put! stream message)))

(defmethod producer :stream
  [config]
  (map->StreamProducer {}))
