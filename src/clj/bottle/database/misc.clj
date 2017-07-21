(ns bottle.database.misc
  (:require [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(defn drop-table!
  [db table]
  (try
    (jdbc/db-do-commands db false (str "drop table " (name table)))
    (catch java.sql.BatchUpdateException ex
      (throw (.getNextException ex)))))
