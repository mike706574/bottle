(ns bottle.users.jdbc
  (:require [bottle.database.misc :as misc]
            [bottle.users :refer [user-manager]]
            [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [taoensso.timbre :as log])
  (:import [bottle.users UserManager]))

(def user-table-ddl
  (jdbc/create-table-ddl :user
                         [[:id :serial "PRIMARY KEY"]
                          [:username "varchar(32)" "NOT NULL"]
                          [:password "char(128)" "NOT NULL"]]))

(defn create-user-table! [db] (jdbc/db-do-commands db [user-table-ddl]))

(comment
  (try
    (create-user-table! db)
    (catch java.sql.BatchUpdateException ex
      (log/debug (.getNextException ex) "OK")))

  (misc/drop-table! db "drop table bar"))

(defn find-by-username
  [db username]
  (jdbc/query db ["select password as encrypted-password from users where username = ?" username]))

(defrecord JdbcUserManager [db]
  UserManager
  (add! [this {:keys [:bottle/username :bottle/password]}]
    (let [password (hashers/encrypt password)]
      (jdbc/insert! db :user {:username username :password password})
      {:bottle/username username}))

  (authenticate [this {:keys [:bottle/username :bottle/password]}]
    (when-let [user (find-by-username db username)]
      (when (hashers/check password (:encrypted-password user))
        (dissoc user :encrypted-password)))))

(comment
  (defmethod user-manager :jdbc
    [config]
    nil))
