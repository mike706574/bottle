(ns bottle.users
  (:require [clojure.spec.alpha :as s]))

(s/def :bottle/username string?)
(s/def :bottle/password string?)
(s/def :bottle/credentials (s/keys :req [:bottle/username :bottle/password]))

(defprotocol UserManager
  "Abstraction around user storage and authentication."
  (add! [this user] "Adds a user.")
  (authenticate [this credentials] "Authenticates a user."))

(s/def :bottle/user-manager (partial satisfies? UserManager))

(s/fdef add!
  :args (s/cat :user-manager :bottle/user-manager
               :credentials :bottle/credentials)
  :ret :bottle/credentials)

(defmulti user-manager :bottle/user-manager-type)

(defmethod user-manager :default
  [{user-manager-type :bottle/user-manager-type}]
  (throw (ex-info (str "Invalid user manager type: " (name user-manager-type))
                  {:user-manager-type user-manager-type})))

#_(s/fdef user-manager
  :args (s/cat :config map?)
  :ret (partial satisfies? UserManager))
