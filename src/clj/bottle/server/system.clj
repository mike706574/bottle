(ns bottle.server.system
  (:require [bottle.event-handler :as event-handler]
            [bottle.event-manager :as event-manager]
            [bottle.message :as message]
            [bottle.message-handler :as message-handler]
            [bottle.notification :as notification]
            [bottle.user-manager :as user-manager]
            [bottle.util :as util]

            [bottle.messaging.consumer :as consumer]
            [bottle.messaging.consumer.activemq]
            [bottle.messaging.consumer.rabbitmq]
            [bottle.messaging.producer.activemq]
            [bottle.messaging.producer.rabbitmq]

            [bottle.server.connection :as conn]
            [bottle.server.handler :as server-handler]
            [bottle.server.service :as service]

            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [manifold.bus :as bus]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]))

(defn configure-logging!
  [{:keys [:bottle/id :bottle/log-path] :as config}]
  (let [log-file (str log-path "/" id "-" (util/uuid))]
    (log/merge-config!
     {:appenders {:spit (appenders/spit-appender
                         {:fname log-file})}})))

;; messaging
(s/def :bottle/broker-type #{:rabbit-mq :active-mq})
(s/def :bottle/broker-path string?)
(s/def :bottle/queue-name string?)
(s/def :bottle/messaging-config (s/keys :req [:bottle/broker-type
                                              :bottle/broker-path
                                              :bottle/queue-name]))
;; event
(s/def :bottle/id string?)
(s/def :bottle/category keyword?)
(s/def :bottle/event-template (s/and (s/keys :req [:bottle/category])
                                     #(not (contains? % :bottle/id))))
(s/def :bottle/event (s/keys :req [:bottle/id
                                   :bottle/category
                                   :bottle/time]))

;; app
(s/def :bottle/id string?)
(s/def :bottle/port integer?)
(s/def :bottle/log-path string?)
(s/def :bottle/event-content-type string?)
(s/def :bottle/event-messaging :bottle/messaging-config)

(s/def :bottle/config (s/keys :req [:bottle/id
                                    :bottle/port
                                    :bottle/log-path
                                    :bottle/event-content-type
                                    :bottle/event-messaging]))

(defn process-event
  [event]
  (log/info (str "Processing event:\n" (util/pretty event)))
  event)

(defn system
  [config]
  (if-let [validation-failure (s/explain-data :bottle/config config)]
    (do (log/error (str "Invalid configuration:\n"
                        (util/pretty config)
                        "Validation failure:\n"
                        (util/pretty validation-failure)))
        (throw (ex-info "Invalid configuration." {:config config
                                                  :validation-failure validation-failure})))
    (let [{:keys [:bottle/id :bottle/event-messaging]} config]
      (log/info (str "Building " id "."))
      (configure-logging! config)
      {:event-bus (bus/event-bus)
       :events (ref {})

       ;; Event processing
       :event-function process-event
       :event-manager (event-manager/event-manager config)
       :event-handler (event-handler/event-handler config)

       ;; Messaging
       :event-message-handler (message-handler/event-message-handler config)
       :event-consumer (component/using
                        (consumer/consumer event-messaging)
                        {:handler :event-message-handler})

       :user-manager (user-manager/user-manager config)

       ;; HTTP
       :connections (atom {})
       :conn-manager (conn/manager config)
       :handler-factory (server-handler/factory config)
       :app (component/using (service/aleph-service config) [:event-consumer])})))

(s/fdef system
  :args (s/cat :config :bottle/config))
