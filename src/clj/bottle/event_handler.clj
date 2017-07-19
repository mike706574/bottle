(ns bottle.event-handler
  (:require [bottle.util :as util]
            [bottle.event-manager :as event-manager]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [manifold.bus :as bus]
            [taoensso.timbre :as log]))

(defprotocol EventHandler
  "Handles events."
  (handle-event [this event] "Handles the given event.")
  (close-event [this id] "Closes an event."))

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
                         (event-manager/add! manager)
                         (function))]
          (log/trace (str "Publishing event creation:\n" (util/pretty event)))
          (bus/publish! bus :all event)
          (bus/publish! bus (:bottle/category event) event)
          {:status :ok :event event}))
      (catch Exception e
        (log/error e (str "Exception thrown while processing event:\n"
                          event))
        {:status :exception})))
  (close-event [this id]
    (if-let [event (event-manager/close! event-manager id)]
      (do
        (log/trace (str "Closed event:\n" (util/pretty event)) )
        ;; Publish to all
        ;; Publish to category bus
        {:status :ok})
      {:status :missing}
      )
    ))

(defn event-handler
  [config]
  (component/using
   (map->BasicEventHandler {})
   {:manager :event-manager
    :function :event-function
    :bus :event-bus}))
