(ns bottle.server.system
  (:require [bottle.messaging.consumer :as consumer]
            [bottle.api.event-handler :as event-handler]
            [bottle.api.event-manager :as event-manager]
            [bottle.server.connection :as conn]
            [bottle.server.handler :as server-handler]
            [bottle.message :as message]
            [bottle.server.service :as service]
            [bottle.util :as util]
            [clojure.spec.alpha :as s]
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
(s/def :bottle/broker-type keyword?)
(s/def :bottle/broker-path string?)
(s/def :bottle/queue-name string?)

;; event
(s/def :bottle/event-id keyword?)
(s/def :bottle/event-type keyword?)
(s/def :bottle/event-template (s/and (s/keys :req [:bottle/event-type])
                                     #(not (contains? % [:bottle/event-id :bottle/event-time]))))
(s/def :bottle/event (s/keys :req [:bottle/event-id
                                   :bottle/event-type
                                   :bottle/event-date]))

;; event
(s/def :bottle/event-id keyword?)
(s/def :bottle/event-type keyword?)
(s/def :bottle/event (s/keys :req [:bottle/event-type]))

;; app
(s/def :bottle/id string?)
(s/def :bottle/port integer?)
(s/def :bottle/log-path string?)
(s/def :bottle/event-content-type string?)
(s/def :bottle/event-messaging (s/keys :req [:bottle/broker-type
                                             :bottle/broker-path
                                             :bottle/queue-name]))

(s/def :bottle/config (s/keys :req [:bottle/id
                                    :bottle/port
                                    :bottle/log-path
                                    :bottle/event-content-type
                                    :bottle/event-messaging]))

(defn process-event
  [event]
  (println "Event:")
  (clojure.pprint/pprint event))

(defn system
  [config]
  (if-let [validation-failure (s/explain-data :bottle/config config)]
    (do (log/error (str "Invalid configuration:\n"
                        (util/pretty config)
                        "Validation failure:\n"
                        (util/pretty validation-failure)))
        (throw (ex-info "Invalid configuration." config)))
    (let [{:keys [:bottle/id :bottle/event-messaging]} config]
      (log/info (str "Building " id "."))
      (println event-messaging)
      (configure-logging! config)
      {:event-bus (bus/event-bus)
       :events (ref {})
       :message-handler (event-handler/event-handler config)
       :event-consumer (consumer/consumer event-messaging)
       :event-function process-event
       :event-manager (event-manager/event-manager config)
       :connections (atom {})
       :conn-manager (conn/manager config)
       :handler-factory (server-handler/factory config)
       :app (service/aleph-service config)})))
