(ns bottle.api.event-handler
  (:require [bottle.util :as util]
            [bottle.message :as message]
            [bottle.messaging.handler :as handler]
            [bottle.api.event-manager :as event-manager]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:import [bottle.messaging.handler MessageHandler]))

(defrecord EventMessageHandler [content-type manager function]
  MessageHandler
  (handle-message [this message]
    (when-let [decoded-message (if (map? message)
                                 message
                                 (try
                                   (message/decode content-type message)
                                   (catch Exception e
                                     (.printStackTrace e)
                                     (log/debug (str "Failed to decode message:\n"
                                                     message)))))]
      (try
        (if-let [validation-failure (s/explain-data :bottle/event decoded-message)]
          {:status :invalid
           :validation-failure validation-failure}
          (let [event (event-manager/store manager decoded-message)]
            (function event)
            {:status :ok
             :event event}))
        (catch Exception e
          (log/error e (str "Exception thrown while handling message:\n"
                            decoded-message))
          {:status :exception})))))

(defn event-handler
  [config]
  (component/using
   (map->EventMessageHandler {:content-type (:bottle/event-content-type config)})
   {:manager :event-manager
    :function :event-function}))
