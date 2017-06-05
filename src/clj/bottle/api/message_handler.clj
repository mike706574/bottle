(ns bottle.api.message-handler
  (:require [bottle.message :as message]
            [bottle.messaging.handler :as handler]
            [bottle.api.event-handler :as event-handler]
            [clojure.spec.alpha :as s]
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

(defrecord EventMessageHandler [content-type handler]
  MessageHandler
  (handle-message [this message]
    (log/trace "Decoding event message.")
    (if-let [decoded-message (decode-message content-type message)]
      (event-handler/handle-event handler decoded-message)
      (log/error (str "Failed to decode message: " (String. message "UTF-8"))))))

(defn event-message-handler
  [{content-type :bottle/event-content-type}]
  (component/using
   (map->EventMessageHandler {:content-type content-type})
   {:handler :event-handler}))
