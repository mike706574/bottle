(ns bottle.server.handler
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [bottle.server.api.handler :as api-handler]))

(defprotocol HandlerFactory
  "Builds a request handler."
  (handler [this]))

(defrecord BottleHandlerFactory [event-content-type
                                 event-bus
                                 event-manager
                                 conn-manager]
  HandlerFactory
  (handler [this]
    (api-handler/handler this)))

(defn factory
  [config]
  (component/using
   (map->MiloHandlerFactory {:event-content-type (:bottle/event-content-type config)})
   [:event-bus :user-manager :event-manager :conn-manager :event-handler]))
