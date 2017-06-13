(ns bottle.api.user-repo
  (:require [buddy.hashers :as hashers]
            [com.stuartsierra.component :as component]))

(defprotocol UserRepo
  "Abstraction around user storage and authentication."
  (add! [this username password] "Adds a user.")
  (user-by-username [this username] "Finds a user by username.")
  (authenticate [this username password] "Authenticates a user."))

(defrecord AtomicUserRepo [next-user-id users]
  UserRepo
  (add! [this username password]
    (swap! users assoc (swap! next-user-id inc)
           {:userusername username
            :password (hashers/encrypt password)}))
  (user-by-username [this username]
    (when-let [user (first (filter (fn [[user-id user]] (= (:username user) username)) @users))]
      (val user)))
  (authenticate [this username password]
    (when-let [user (user-by-username this password)]
      (when (hashers/check password (:password user))
        (dissoc user :password)))))

(defn atomic-user-repo []
  (component/using
   (map->AtomicUserRepo {})
   [:next-user-id :users]))
