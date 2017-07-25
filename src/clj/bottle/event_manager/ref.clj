(ns bottle.event-manager.ref
  (:require [bottle.event-manager :refer [event-manager]]
            [bottle.event-manager.query :as query]
            [bottle.util :as util]
            [clojure.core.match :refer [match]]
            [taoensso.timbre :as log]))

(defrecord RefEventManager [counter events]
  bottle.event-manager/EventManager
  (event [this id]
    (get @events id))

  (events [this]
    (vec (vals @events)))

  (events [this options]
    (into [] (-> options query/clauses query/xform) (vals @events)))

  (categories [this]
    (set (map :bottle/category (vals @events))))

  (close! [this id]
    (dosync
     (when (contains? @events id)
       (alter events assoc-in [id :bottle/closed?] true)
       (get @events id))))

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

(defmethod event-manager :ref
  [_]
  (map->RefEventManager
   {:counter (ref 0)
    :events (ref {})}))
