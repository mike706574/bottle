(ns bottle.api.event-handler
  (:require [bottle.util :as util]
            [bottle.message :as message]
            [bottle.api.event-manager :as event-manager]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defprotocol EventHandler
  "Handles events."
  (handle-event [this data] "Get the next event identifier."))

(defrecord BasicEventHandler [manager content-type function]
  EventHandler
  (handle-event [this message]
    (when-let [decoded-message (try
                                 (message/decode content-type message)
                                 (catch Exception e
                                   (log/debug (str "Failed to decode message:\n"
                                                   message))))]
      (try
        (if-let [validation-failure (s/explain-data :bottle/event decoded-message)]
          (log/error (str "Invalid event:\n"
                          (util/pretty decoded-message)
                          "Validation failure:"
                          (util/pretty validation-failure)))
          ;; TODO: Handle error.
          (let [event (event-manager/store manager decoded-message)]
            (function event)))
        (catch Exception e
          (log/error e (str "Exception thrown while handling message:\n"
                            decoded-message)))))))

(defn event-handler
  [config]
  (component/using
   (map->BasicEventHandler config)
   {:manager :event-manager
    :function :event-function}))
