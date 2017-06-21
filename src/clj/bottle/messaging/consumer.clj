(ns bottle.messaging.consumer
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]))

(defmulti consumer :bottle.messaging/broker-type)

(defmethod consumer :default
  [{broker-type :bottle.messaging/broker-type :as config}]
  (throw (ex-info (str "Invalid broker type: " (name broker-type)) (or config {}))))

#_(s/fdef consumer
  :args (s/cat :config :bottle.messaging/config )
  :ret (partial satisfies? component/Lifecycle))
