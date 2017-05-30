(ns bottle.api.event-manager
  (:require [com.stuartsierra.component :as component]
            [manifold.stream :as s]
            [bottle.util :as util]
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
    (dosync
     (let [id (alter counter inc)
           _ (println data)
           event (assoc data :bottle/event-id id)]
       (alter events assoc id event)
       event))))

(defn event-manager [config]
  (component/using (map->RefEventManager {:counter (ref 0)})
    [:events]))
