(ns bottle.users.jdbc
  (:require [bottle.users :refer [user-manager]]
            [buddy.hashers :as hashers])
  (:import [bottle.users UserManager]))

(defrecord JdbcUserManager []
  UserManager
  (add! [this user]
    nil)

  (authenticate [this {:keys [:bottle/username :bottle/password]}]
    nil))

(defmethod user-manager :jdbc
  [config]
  nil)
