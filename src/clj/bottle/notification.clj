(ns bottle.notification
  (:require [bottle.util :as util]
            [boomerang.message :as message]
            [bottle.messaging.handler :as handler]
            [com.stuartsierra.component :as component]
            [manifold.bus :as bus]
            [taoensso.timbre :as log])
  (:import [bottle.messaging.handler MessageHandler]))

(defn decode-message
  [content-type message]
  (try
    (message/decode content-type message)
    (catch Exception e
      (log/error e (str "Failed to decode message:\n"
                        message)))))

(defrecord NotificationMessageHandler [content-type bus]
  MessageHandler
  (handle-message [this message]
    (when-let [event (decode-message content-type message)]
      (bus/publish! bus :all event)
      (bus/publish! bus (:bottle/category event) event))))

(defn message-handler
  [{content-type :bottle/event-content-type}]
  (map->NotificationMessageHandler
   (component/using
    (map->NotificationMessageHandler {:content-type content-type})
    {:bus :event-bus})))
