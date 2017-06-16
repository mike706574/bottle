(ns bottle.messaging.consumer)

(defmulti consumer :bottle/broker-type)

(defmethod consumer :default
  [{broker-type :bottle/broker-type :as config}]
  (throw (ex-info (str "Invalid broker type: \"" broker-type "\"") (or config {}))))
