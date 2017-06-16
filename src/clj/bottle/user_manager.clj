(ns bottle.user-manager
  (:require [buddy.hashers :as hashers]
            [clojure.spec.alpha :as s]))

(s/def :bottle/username string?)
(s/def :bottle/password string?)
(s/def :bottle/credentials (s/keys :req [:bottle/username :bottle/password]))

(defprotocol UserManager
  "Abstraction around user storage and authentication."
  (add! [this user] "Adds a user.")
  (authenticate [this credentials] "Authenticates a user."))

(s/def :bottle/user-manager (partial UserManager))

(defn ^:private find-by-username
  [users username]
  (when-let [user (first (filter (fn [[user-id user]] (= (:bottle/username user) username)) @users))]
    (val user)))

(defrecord AtomicUserManager [counter users]
  UserManager
  (add! [this user]
    (swap! users assoc (str (swap! counter inc))
           (update user :bottle/password hashers/encrypt)))

  (authenticate [this {:keys [:bottle/username :bottle/password]}]
    (when-let [user (find-by-username users username)]
      (when (hashers/check password username)
        (dissoc user :bottle/password)))))

(defn user-manager [_]
  (AtomicUserManager. (atom 0) (atom {})))
