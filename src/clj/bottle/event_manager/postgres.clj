(ns bottle.event-manager.postgres
  (:require [bottle.event-manager :refer [event-manager]]
            [bottle.database.misc :as misc]
            [bottle.util :as util]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(def event-table-ddl
  (jdbc/create-table-ddl :event
                         [[:id :serial "PRIMARY KEY"]
                          [:category "varchar(32)"]
                          [:closed :boolean "DEFAULT false" ]
                          [:time :timestamp "DEFAULT now()"]
                          [:data :json]]))

(defn ^:private create-event-table!
  [db]
  (try
    (jdbc/db-do-commands db [event-table-ddl])
    (catch java.sql.BatchUpdateException ex
      (throw (.getNextException ex)))))

(defn ^:private select-events
  [db]
  (letfn [(event [{data :data :as row}]
            (-> row
                (dissoc :data)
                (merge data)))]
    (jdbc/query db ["select id as \"bottle/id\", time as \"bottle/time\", category as \"bottle/category\", data from event"] {:row-fn event})))

(defn ^:private select-categories
  [db]
  (jdbc/query db ["select distinct category from event"]))

(defn ^:private insert-event!
  [db template]
  (some-> db
          (jdbc/insert! :event {:category (:bottle/category template)
                                :data (dissoc template :bottle/category)})
          first))

(defrecord PostgresEventManager [db]
  bottle.event-manager/EventManager
  (event [this id]
    nil)

  (events [this]
    (select-events db))

  (events [this options]
    nil)

  (categories [this]
    (select-categories db))

  (close! [this id]
    nil)

  (add! [this template]
    (insert-event! db template)))

(defmethod event-manager :jdbc
  [config]
  (map->PostgresEventManager {}))

(comment
  (create-event-table! db)
  (misc/drop-table! db :event)
  (insert-event! db {:bottle/category "foo" :foo 1})

  (select-events db)
  (select-categories db)


  )
