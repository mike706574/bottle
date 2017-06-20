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
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :host "localhost"
   :port 5432
   :dbname "foo"
   :user "postgres"
   :password "postgres"})

(comment
  (misc/recreate-database
   db "foo" create-user-table
   )

  )

(defrecord JdbcUserManager [db]
  UserManager
  (add! [this {:keys [:bottle/username :bottle/password]}]
    (let [password (hashers/encrypt password)]
      (jdbc/insert! db :user {:username username :password password})
      {:bottle/username username}))

  (authenticate [this {:keys [:bottle/username :bottle/password]}]
    (jdbc/execute! [])
    (jdbc/query db ["select password as encrypted-password from users where username = ?" username])

    ))

(comment
  (defmethod user-manager :jdbc
    [config]
    nil))
