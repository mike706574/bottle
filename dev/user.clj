(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
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
   [clojure.spec.alpha :as spec]
   [clojure.spec.test.alpha :as stest]

   [aleph.http :as http]
   [manifold.stream :as s]
   [manifold.deferred :as d]
   [manifold.bus :as bus]

   [bottle.client :as client]
   [bottle.users :as users]
   [bottle.server.system :as system]
   [bottle.message :as message]
   [bottle.messaging.producer :as producer]
   [taoensso.timbre :as log]))

(log/set-level! :trace)

(stest/instrument)

(def messaging-config {:bottle/broker-type :rabbit-mq
                       :bottle/broker-path "localhost"
                       :bottle/queue-name "bottle-1"
                       :bottle/handler :event-message-handler})

(def port 8001)
(def content-type "application/transit+json")

(def config {:bottle/id "bottle-server"
             :bottle/port port
             :bottle/log-path "/tmp"
             :bottle/event-content-type content-type
             :bottle/event-messaging messaging-config
             :bottle/user-manager-type :atomic
             :bottle/users {"mike" "rocket"}})

(defonce system nil)

(def url (str "http://localhost:" port))
(def ws-url (str "ws://localhost:" port "/api/websocket"))

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
