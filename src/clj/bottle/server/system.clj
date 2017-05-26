(ns bottle.server.system
  (:require [manifold.bus :as bus]
            [bottle.api.event-consumer :as event-consumer]
            [bottle.api.event-manager :as event-manager]
            [bottle.server.connection :as conn]
            [bottle.server.handler :as handler]
            [bottle.server.service :as service]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]))

(defn uuid [] (.toString (java.util.UUID/randomUUID)))

(defn system
  [{:keys [id log-path] :as config}]
  (log/info "Building system.")
  (let [log-file (str log-path "/" id "-" (uuid))]
      (log/merge-config!
       {:appenders {:spit (appenders/spit-appender
                           {:fname log-file})}})
    {:event-bus (bus/event-bus)
     :connections (atom {})
     :events (ref {})
     :conn-manager (conn/manager)
     :event-manager (event-manager/event-manager)
     :handler-factory (handler/factory)
     :app (service/aleph-service config)}))
