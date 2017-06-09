(ns bottle.api.event-handler-test
  (:require [bottle.api.event-handler :as event-handler]
            [bottle.api.event-manager :as manager]
            [bottle.macros :refer [with-system]]
            [bottle.messaging.handler :as handler]
            [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]))

(def config {:bottle/event-content-type "application/edn"})

(defn system [config]
  (let [event-arg (atom nil)]
    {:event event-arg
     :events (ref {})
     :event-function (fn [event] (reset! event-arg event))
     :event-manager (manager/event-manager config)
     :event-handler (event-handler/event-handler config)}))

(deftest messages
  (with-system (system config)
    (let [{:keys [event events event-handler]} system
          message (pr-str {:bottle/category :foo})]
      (handler/handle-message event-handler message)
      (is (= {:bottle/category :foo :bottle/id "1"} @event))
      (is (= {"1" {:bottle/category :foo :bottle/id "1"}} @events)))))
