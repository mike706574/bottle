(ns bottle.api.event-handler
  (:require [bottle.util :as util]
            [bottle.api.event-manager :as event-manager]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [manifold.bus :as bus]
            [taoensso.timbre :as log]))

(defprotocol EventHandler
  "Handles events."
  (handle-event [this event] "Handles the given event.."))

(defrecord BasicEventHandler [manager bus function]
  EventHandler
  (handle-event
    [this event]
    (try
      (if-let [validation-failure (s/explain-data :bottle/event-template event)]
        (do (log/warn (str "Invalid event:\n"
                            (util/pretty event)
                            "Validation failure:\n"
                            (util/pretty validation-failure)))
            {:status :invalid
             :validation-failure validation-failure})
        (let [event (->> event
                         (event-manager/store manager)
                         (function))]
          (log/trace (str "Publishing event:\n"
                          (util/pretty event)))
          (bus/publish! bus :all event)
          (bus/publish! bus (:bottle/event-type event) event)
          {:status :ok :event event}))
      (catch Exception e
        (log/error e (str "Exception thrown while processing event:\n"
                          event))
        {:status :exception}))))

(defn event-handler
  [config]
  (component/using
   (map->BasicEventHandler {})
   {:manager :event-manager
    :function :event-function
    :bus :event-bus}))
