(ns bottle.server.system
  (:require [bottle.event-handler :as event-handler]
            [bottle.event-manager :as event-manager]
            [bottle.message :as message]
            [bottle.message-handler :as message-handler]
            [bottle.notification :as notification]
            [bottle.users :as users]
            [bottle.util :as util]

            [bottle.messaging.consumer :as consumer]

            ;; TODO
            [bottle.messaging]

            [bottle.messaging.consumer [activemq rabbitmq stream]]
            [bottle.messaging.producer [activemq rabbitmq stream]]

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
;; app
(s/def :bottle/id string?)
(s/def :bottle/port integer?)
(s/def :bottle/log-path string?)
(s/def :bottle/event-content-type string?)
(s/def :bottle/event-messaging :bottle.messaging/config)
(s/def :bottle/user-manager-type #{:atomic})
(s/def :bottle/users (s/map-of :bottle/username :bottle/password))
(s/def :bottle/config (s/keys :req [:bottle/id
                                    :bottle/port
                                    :bottle/log-path
                                    :bottle/user-manager-type
                                    :bottle/event-content-type
                                    :bottle/event-messaging]
                              :opt [:bottle/users]))

(defn process-event
  [event]
  (log/info (str "Processing event:\n" (util/pretty event)))
  event)

(defn event-consumer
  [{event-messaging :bottle/event-messaging}]
  (component/using
    (consumer/consumer event-messaging)
    {:handler :event-message-handler}))



(defn build
  [config]
  (log/info (str "Building " (:bottle/id config) "."))
  (configure-logging! config)
  {
   ;; Per-event behavior
   :event-function process-event

   ;; Event storage
   :event-manager (event-manager/event-manager config)

   ;; User storage
   :user-manager (users/user-manager config)

   ;; Event processing
   :event-consumer (event-consumer config)
   :event-bus (bus/event-bus)
   :event-handler (event-handler/event-handler config)

   ;; Messaging
   :event-message-handler (message-handler/event-message-handler config)

   ;; HTTP
   :conn-manager (conn/manager config)
   :handler-factory (server-handler/factory config)
   :app (component/using (service/aleph-service config) [:event-consumer])})

(defn system
  [config]
  (if-let [validation-failure (s/explain-data :bottle/config config)]
    (do (log/error (str "Invalid configuration:\n"
                        (util/pretty config)
                        "Validation failure:\n"
                        (util/pretty validation-failure)))
        (throw (ex-info "Invalid configuration." {:config config
                                                  :validation-failure validation-failure})))
    (build config)))

(s/fdef system
  :args (s/cat :config :bottle/config)
  :ret map?)
