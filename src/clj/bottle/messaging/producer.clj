(ns bottle.messaging.producer
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]))

(defprotocol Producer
  "Produces messages."
  (produce [this message] "Produce a message."))

(defmulti producer :bottle.messaging/broker-type)

(defmethod producer :default
  [{broker-type :bottle.messaging/broker-type :as config}]
  (throw (ex-info (str "Invalid broker type: \"" broker-type "\"") (or config {}))))

(s/def :bottle/producer (partial satisfies? Producer))

;; TODO
(comment
  (s/fdef producer
    :args (s/cat :config :bottle.messaging/config )
    :ret :bottle/producer))
