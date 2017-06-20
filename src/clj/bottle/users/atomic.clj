(ns bottle.users.atomic
  (:require [bottle.users :as users :refer [user-manager]]
            [buddy.hashers :as hashers])
  (:import [bottle.users UserManager]))

(defn ^:private find-by-username
  [users username]
  (when-let [user (first (filter (fn [[user-id user]] (= (:bottle/username user) username)) @users))]
    (val user)))

(defrecord AtomicUserManager [counter users]
  UserManager
  (add! [this user]
    (swap! users assoc (str (swap! counter inc))
           (update user :bottle/password hashers/encrypt))
    (dissoc user :bottle/password))

  (authenticate [this {:keys [:bottle/username :bottle/password]}]
    (when-let [user (find-by-username users username)]
      (when (hashers/check password (:bottle/password user))
        (dissoc user :bottle/password)))))

(defmethod user-manager :atomic
  [config]
  (let [user-manager (AtomicUserManager. (atom 0) (atom {}))]
    (when-let [users (:bottle/users config)]
      (doseq [[username password] users]
        (users/add! user-manager {:bottle/username username
                                  :bottle/password password})))
    user-manager))
