(ns bottle.event-manager.jdbc
  (:require [bottle.event-manager :refer [event-manager]]
            [bottle.util :as util]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(defrecord JdbcEventManager [db]
  bottle.event-manager/EventManager
  (event [this id]
    (jdbc/query db ["select password as encrypted-password from users where username = ?" username])
    )

  (events [this]
    @events)

  (events [this options]
    (let [xforms (->> factories
                     (map #(% options))
                     (filter identity))]
      (into [] (apply comp xforms) (vals @events))))

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

(defmethod event-manager :jdbc
  [_]
  (map->RefEventManager {}))
