(ns bottle.connection-pool
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component])
  (:import [com.mchange.v2.c3p0 ComboPooledDataSource]))

;; TODO
(s/def :bottle/database-config identity)

(defrecord ConnectionPool [datasource config]
  component/Lifecycle
  (start [this]
    (let [{:keys [subprotocol subname classname username password
                  excess-timeout idle-timeout minimum-pool-size maximum-pool-size
                  test-connection-query
                  idle-connection-test-period
                  test-connection-on-checkin
                  test-connection-on-checkout]
           :or {excess-timeout (* 30 60)
                idle-timeout (* 3 60 60)
                minimum-pool-size 3
                maximum-pool-size 15
                test-connection-query nil
                idle-connection-test-period 0
                test-connection-on-checkin false
                test-connection-on-checkout false}
           :as spec} config
          datasource (doto (ComboPooledDataSource.)
                       (.setDriverClass classname)
                       (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
                       (.setUser username)
                       (.setPassword password)
                       (.setMaxIdleTimeExcessConnections excess-timeout)
                       (.setMaxIdleTime idle-timeout)
                       (.setMinPoolSize minimum-pool-size)
                       (.setMaxPoolSize maximum-pool-size)
                       (.setIdleConnectionTestPeriod idle-connection-test-period)
                       (.setTestConnectionOnCheckin test-connection-on-checkin)
                       (.setTestConnectionOnCheckout test-connection-on-checkout)
                       (.setPreferredTestQuery test-connection-query))])
    (assoc this :datasource datasource))
  (stop [this]
    (.close datasource)
    this))

(defn connection-pool
  [config]
  (map->ConnectionPool {:config config}))
