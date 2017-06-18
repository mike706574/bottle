(ns bottle.server.system
  (:require [bottle.event-handler :as event-handler]
            [bottle.event-manager :as event-manager]
            [bottle.message :as message]
            [bottle.message-handler :as message-handler]
            [bottle.notification :as notification]
            [bottle.user-manager :as user-manager]
            [bottle.util :as util]

            [bottle.messaging.consumer :as consumer]

            ;; TODO
            [bottle.messaging.consumer.activemq]
            [bottle.messaging.consumer.rabbitmq]
            [bottle.messaging.consumer.stream]
            [bottle.messaging.producer.activemq]
            [bottle.messaging.producer.rabbitmq]
            [bottle.messaging.producer.stream]

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
(s/def :bottle/broker-type #{:rabbit-mq :active-mq :stream})
(s/def :bottle/broker-path string?)
(s/def :bottle/queue-name string?)
(s/def :bottle/messaging-config (s/keys :req [:bottle/broker-type]
                                        :opt [:bottle/broker-path
                                              :bottle/queue-name]))
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

(defn event-consumer
  [{event-messaging :bottle/event-messaging}]
  (component/using
    (consumer/consumer event-messaging)
    {:handler :event-message-handler}))

(defn system
  [config]
  (if-let [validation-failure (s/explain-data :bottle/config config)]
    (do (log/error (str "Invalid configuration:\n"
                        (util/pretty config)
                        "Validation failure:\n"
                        (util/pretty validation-failure)))
        (throw (ex-info "Invalid configuration." {:config config
                                                  :validation-failure validation-failure})))
    (do (log/info (str "Building " (:id config) "."))
        (configure-logging! config)
        {
         ;; Per-event behavior
         :event-function process-event

         ;; Event storage
         :event-manager (event-manager/event-manager config)

         ;; User storage
         :user-manager (user-manager/user-manager config)

         ;; Event processing
         :event-consumer (event-consumer config)
         :event-bus (bus/event-bus)
         :event-handler (event-handler/event-handler config)

         ;; Messaging
         :event-message-handler (message-handler/event-message-handler config)

         ;; HTTP
         :connections (atom {})
         :conn-manager (conn/manager config)
         :handler-factory (server-handler/factory config)
         :app (component/using (service/aleph-service config) [:event-consumer])})))

(s/fdef system
  :args (s/cat :config :bottle/config)
  :ret map?)
