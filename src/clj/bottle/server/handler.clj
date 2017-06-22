(ns bottle.server.handler
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [bottle.server.api.handler :as api-handler]))

(defprotocol HandlerFactory
  "Builds a request handler."
  (handler [this]))

(defrecord BottleHandlerFactory [conn-manager
                                 event-bus
                                 event-content-type
                                 event-handler
                                 event-manager
                                 user-manager]
  HandlerFactory
  (handler [this]
    (api-handler/handler this)))

(defn factory
  [{:keys [:bottle/event-content-type]}]
  (component/using
   (map->BottleHandlerFactory {:event-content-type event-content-type})
   [:conn-manager :event-bus :event-handler :event-manager :user-manager :authenticator]))
