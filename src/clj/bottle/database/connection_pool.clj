(ns bottle.database.connection-pool
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component])
  (:import [com.mchange.v2.c3p0 ComboPooledDataSource]))

(s/def :bottle.database/subprotocol #{"postgresql" "mysql"})
(s/def :bottle.database/driver-class #{"org.postgresql.Driver" "com.mysql.jdbc.Driver"})
(s/def :bottle.database/host string?)
(s/def :bottle.database/port integer?)
(s/def :bottle.database/database string?)
(s/def :bottle.database/username string?)
(s/def :bottle.database/password string?)
(s/def :bottle.database/excess-timeout number?)
(s/def :bottle.database/idle-timeout number?)
(s/def :bottle.database/minimum-pool-size integer?)
(s/def :bottle.database/test-connection-query (s/or :not-present nil?
                                                    :present string?))
(s/def :bottle.database/idle-connection-test-period number?)
(s/def :bottle.database/test-connection-on-checkin boolean?)
(s/def :bottle.database/test-connection-on-checkout boolean?)

(s/def :bottle.database/config (s/keys :req [:bottle.database/subprotocol
                                             :bottle.database/driver-class
                                             :bottle.database/host
                                             :bottle.database/database
                                             :bottle.database/username
                                             :bottle.database/password]
                                       :opt [:bottle.database/port
                                             :bottle.database/excess-timeout
                                             :bottle.database/idle-timeout
                                             :bottle.database/minimum-pool-size
                                             :bottle.database/maximum-pool-size
                                             :bottle.database/test-connection-query
                                             :bottle.database/idle-connection-test-period
                                             :bottle.database/test-connection-on-checkin
                                             :bottle.database/test-connection-on-checkout]))

(defrecord ConnectionPool [datasource config]
  component/Lifecycle
  (start [this]
    (let [{:keys [:bottle.database/subprotocol
                  :bottle.database/host
                  :bottle.database/port
                  :bottle.database/database
                  :bottle.database/driver-class
                  :bottle.database/username
                  :bottle.database/password
                  :bottle.database/excess-timeout
                  :bottle.database/idle-timeout
                  :bottle.database/minimum-pool-size
                  :bottle.database/maximum-pool-size
                  :bottle.database/test-connection-query
                  :bottle.database/idle-connection-test-period
                  :bottle.database/test-connection-on-checkin
                  :bottle.database/test-connection-on-checkout]
           :or {excess-timeout (* 30 60)
                idle-timeout (* 3 60 60)
                minimum-pool-size 3
                maximum-pool-size 15
                test-connection-query nil
                idle-connection-test-period 0
                test-connection-on-checkin false
                test-connection-on-checkout false}
           :as spec} config
          ds (doto (ComboPooledDataSource.)
               (.setDriverClass driver-class)
               (.setJdbcUrl (str "jdbc:" subprotocol "://" (if port (str host ":" port) host) "/" database))
               (.setUser username)
               (.setPassword password)
               (.setMaxIdleTimeExcessConnections excess-timeout)
               (.setMaxIdleTime idle-timeout)
               (.setMinPoolSize minimum-pool-size)
               (.setMaxPoolSize maximum-pool-size)
               (.setIdleConnectionTestPeriod idle-connection-test-period)
               (.setTestConnectionOnCheckin test-connection-on-checkin)
               (.setTestConnectionOnCheckout test-connection-on-checkout)
               (.setPreferredTestQuery test-connection-query))]
      (assoc this :datasource ds)))
  (stop [this]
    (.close datasource)
    this))

(s/def :bottle/connection-pool (partial satisfies? ConnectionPool))

(defn connection-pool
  [config]
  (map->ConnectionPool {:config config}))

(s/fdef connection-pool
  :args (s/cat :config :bottle.database/config)
  :ret :bottle/connection-pool)
