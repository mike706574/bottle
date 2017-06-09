(ns bottle.server.handler
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [bottle.server.api.handler :as api-handler]))

(defprotocol HandlerFactory
  "Builds a request handler."
  (handler [this]))

(defrecord MiloHandlerFactory [event-content-type
                               event-bus
                               event-manager
                               conn-manager]
  HandlerFactory
  (handler [this]
    (let [api (api-handler/handler this)]
      (fn [{uri :uri :as request}]
        (if (str/starts-with? uri "/api")
          (api request)
          (throw (RuntimeException. "Please help me.")))))))

(defn factory
  [config]
  (component/using
   (map->MiloHandlerFactory {:event-content-type (:bottle/event-content-type config)})
   [:event-bus :event-manager :conn-manager :event-handler]))
