(ns bottle.database.misc
  (:require [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(defn create-database
  [db db-name]
  (try
    (jdbc/db-do-commands db false (str "create database " db-name))
    (catch Exception ex
      (log/error ex "Error creating database."))))

(defn drop-database
  [db db-name]
  (try
    (jdbc/db-do-commands db false (str "drop database " db-name))
    (catch Exception ex
      (log/error ex "Error dropping database."))))

(defn recreate-database
  [db db-name & commands]
  (drop-database db db-name)
  (create-database db db-name)
  (try
    (apply jdbc/db-do-commands db commands)
    (catch Exception ex
      (log/error ex "Error executing commands."))))
