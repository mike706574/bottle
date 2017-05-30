(ns bottle.messaging.handler)

(defprotocol MessageHandler
  "Handles messages."
  (handle-message [this message] "Handles the given message."))
