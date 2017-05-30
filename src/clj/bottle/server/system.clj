(ns bottle.server.system
  (:require [bottle.api.event-consumer :as event-consumer]
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

(s/def :bottle/event-id keyword?)
(s/def :bottle/event-type keyword?)
(s/def :bottle/event (s/keys :req [:bottle/event-type]))

(s/def :bottle/id string?)
(s/def :bottle/port integer?)
(s/def :bottle/log-path string?)
(s/def :bottle/event-content-type string?)

(s/def :bottle/app-config (s/keys :req [:bottle/id
                                        :bottle/port
                                        :bottle/log-path]))

(s/def :bottle/config (s/merge :bottle/messaging-config
                               :bottle/app-config))

(defmulti process-event :bottle/event-type)

(defmethod process-event :foo
  [event]
  (println "foo 2222!"))

(defmethod process-event :default
  [event]
  (println "^_^ Event: " event))

(defn system
  [config]
  (log/info (str "Building " (:bottle/id config) "."))
  (if-let [validation-failure (s/explain-data :bottle/config config)]
    (do
      (log/error (str "Invalid configuration:\n"
                      (util/pretty config)
                      "Validation failure:\n"
                      (util/pretty validation-failure)))
      (throw (ex-info "Invalid configuration." config)))
    (do (configure-logging! config)
        {:event-bus (bus/event-bus)
         :events (ref {})

         :event-function process-event
         :event-consumer (event-consumer/event-consumer config)
         :event-manager (event-manager/event-manager config)
         :event-handler (event-handler/event-handler config)

         :connections (atom {})
         :conn-manager (conn/manager config)
         :handler-factory (server-handler/factory config)
         :app (service/aleph-service config)})))
