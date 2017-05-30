(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [aleph.http :as http]
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [clojure.walk :as walk]
   [cognitect.transit :as transit]
   [com.stuartsierra.component :as component]
   [manifold.stream :as s]
   [manifold.deferred :as d]
   [manifold.bus :as bus]
   [bottle.server.system :as system]
   [bottle.message :as message]
   [taoensso.timbre :as log]
   ;; Messaging
   [clamq.activemq :as amq]
   [clamq.protocol.connection :as conn]
   [clamq.protocol.consumer :as consumer]
   [clamq.protocol.producer :as producer]))

(log/set-level! :trace)

(def config {:bottle/id "bottle-server"
             :bottle/port 8002
             :bottle/log-path "/tmp"
             :bottle/event-broker-type :active-mq
             :bottle/event-broker-path "tcp://localhost:61616"
             :bottle/event-content-type "application/transit+msgpack"
             :bottle/event-endpoint "foo"})

(defonce system nil)

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (alter-var-root #'system (constantly (system/system config)))
  :init)

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (try
    (alter-var-root #'system component/start-system)
    :started
    (catch Exception ex
      (log/error (or (.getCause ex) ex) "Failed to start system.")
      :failed)))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop-system s))))
  :stopped)

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after `go))

(defn restart
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (go))

(defn connections [] @(:connections system))


(comment


  (def cconn (amq/activemq-connection url))
  (def consumer (conn/consumer cconn {:endpoint "foo"
                                      :on-message (fn [message]
                                                    (println message))
                                      :transacted true}))
  (consumer/start consumer)








  )
