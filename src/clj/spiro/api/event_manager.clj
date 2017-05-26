(ns spiro.api.event-manager
  (:require [com.stuartsierra.component :as component]
            [manifold.stream :as s]
            [spiro.server.util :as util]
            [taoensso.timbre :as log]))

(defprotocol EventManager
  "Manages events."
  (events [this] "Retrieve all events.")
  (store [this data] "Get the next event identifier."))

(defrecord RefEventManager [counter events]
  EventManager
  (events [this]
    @events)
  (store [this data]
    (let [id (alter counter inc)
          event (assoc data :spiro/event-id id)]
      (alter events assoc id event)
      event)))

(defn event-manager []
  (component/using (map->RefEventManager {:counter (ref 0)})
    [:events]))
