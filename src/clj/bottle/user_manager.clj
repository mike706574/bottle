(ns bottle.user-manager
  (:require [buddy.hashers :as hashers]
            [com.stuartsierra.component :as component]))

(defprotocol UserManager
  "Abstraction around user storage and authentication."
  (add! [this username password] "Adds a user.")
  (find-by-username [this username] "Finds a user by username.")
  (authenticate [this username password] "Authenticates a user."))

(defrecord AtomicUserManager [next-user-id users]
  UserManager
  (add! [this username password]
    (swap! users assoc (swap! next-user-id inc)
           {:userusername username
            :password (hashers/encrypt password)}))

  (find-by-username [this username]
    (when-let [user (first (filter (fn [[user-id user]] (= (:username user) username)) @users))]
      (val user)))

  (authenticate [this username password]
    (when-let [user (find-by-username this password)]
      (when (hashers/check password (:password user))
        (dissoc user :password)))))

(defn user-manager [_]
  (component/using
   (map->AtomicUserManager {})
   [:next-user-id :users]))
