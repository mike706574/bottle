(ns bottle.database.misc
  (:require [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(defn recreate-database
  [db db-name & commands]
  (try
    (jdbc/db-do-commands db false (str "drop database " db-name))
    (catch java.sql.BatchUpdateException ex
      (log/error (.getNextException ex) "Error dropping database.")))
  (try
    (jdbc/db-do-commands db false (str "create database " db-name))
    (catch java.sql.BatchUpdateException ex
      (log/error (.getNextException ex) "Error creating database.")))
  (try
    (apply jdbc/db-do-commands db commands)
    (catch java.sql.BatchUpdateException ex
      (log/error (.getNextException ex) "Error executing commands.")
      )
    )
)
