(ns bottle.event-manager
  (:require [clojure.spec.alpha :as s]
            [bottle.specs]
            [bottle.util :as util]
            [taoensso.timbre :as log]))

(defprotocol EventManager
  "Manages events."
  (events [this] "Retrieve all events.")
  (add! [this data] "Add an event."))

(defrecord RefEventManager [counter events]
  EventManager
  (events [this]
    @events)
  (add! [this template]
    (dosync
     (let [id (str (alter counter inc))
           event (assoc template
                        :bottle/id id
                        :bottle/time (java.util.Date.)
                        :bottle/closed? false)]
       (log/debug (str "Storing event " event "."))
       (alter events assoc id event)
       event))))

(defn event-manager [config]
  (map->RefEventManager
   {:counter (ref 0)
    :events (ref {})}))

(s/def :bottle/event-manager (partial satisfies? EventManager))

(defn ^:private submap?
  [sub sup]
  (= sub (select-keys sup (keys sub))))

(s/fdef add!
  :args (s/cat :event-manager :bottle/event-manager
               :event-template :bottle/event-template)
  :ret :bottle/event
  :fn #(submap? (-> % :args :event-template) (:ret %)))

(s/fdef events
  :args (s/cat :event-manager :bottle/event-manager)
  :ret (s/map-of string? :bottle/event)
  :fn #(submap? (-> % :args :event-template) (:ret %)))
