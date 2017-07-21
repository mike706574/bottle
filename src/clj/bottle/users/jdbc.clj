(ns bottle.users.jdbc
  (:require [bottle.database.misc :as misc]
            [bottle.users :refer [user-manager]]
            [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc])
  (:import [bottle.users UserManager]))

(def create-user-table
  (jdbc/create-table-ddl :bar
                         [[:id :serial "PRIMARY KEY"]
                          [:username "varchar(32)" "NOT NULL"]
                          [:password "char(128)" "NOT NULL"]]))

(def db
  {:dbtype "postgres"
   :host "localhost"
   :port 5432
   :dbname "postgres"
   :user "postgres"
   :password ""})

(comment
  (misc/drop-database db "bottle")
  (misc/create-database db "bottle" create-user-table)

  )

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
