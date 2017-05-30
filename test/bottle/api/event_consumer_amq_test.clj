(ns bottle.api.event-consumer-amq-test
  (:require [bottle.api.event-consumer :as ec]

            [com.stuartsierra.component :as component]
            [clojure.test :refer [deftest testing is]]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [bottle.server.system :as system]
            [bottle.message :as message]
            [taoensso.timbre :as log]))

(def config {:bottle/event-broker-path "tcp://localhost:61616"
             :bottle/event-endpoint "event-consumer-test"
             :bottle/event-broker-type :active-mq})

(def system (ec/event-consumer config))

(defrecord TestMessageHandler
    Messagehandler)

(deftest connecting

  )
