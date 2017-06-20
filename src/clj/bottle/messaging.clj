(ns bottle.messaging
  (:require [clojure.spec.alpha :as s]))

(s/def :bottle.messaging/broker-type #{:rabbit-mq :active-mq :stream})
(s/def :bottle.messaging/broker-path string?)
(s/def :bottle.messaging/queue-name string?)
(s/def :bottle.messaging/config (s/keys :req [:bottle.messaging/broker-type]
                                        :opt [:bottle.messaging/broker-path
                                              :bottle.messaging/queue-name]))
