(ns bottle.event-handler-test
  (:require [bottle.event-handler :as event-handler]
            [bottle.event-manager :as manager]
            [bottle.macros :refer [with-system]]
            [bottle.messaging.handler :as handler]
            [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]
            [manifold.bus :as bus]))

(defn system [config]
  (let [event-arg (atom nil)]
    {:event-bus (bus/event-bus)
     :event event-arg
     :event-function (fn [event] (reset! event-arg event))
     :event-manager (manager/event-manager config)
     :event-handler (event-handler/event-handler config)}))

(deftest messages
  (with-system (system {})
    (let [{:keys [event event-manager event-handler]} system]
      (event-handler/handle-event event-handler {:bottle/category :foo})
      (is (= {:bottle/category :foo :bottle/id "1"}
             (select-keys @event [:bottle/category :bottle/id])))
      (let [events (manager/events event-manager)]
        (is (= 1 (count events)))
        (is (= {:bottle/category :foo :bottle/id "1"}
               (select-keys (get events "1") [:bottle/category :bottle/id])))))))
