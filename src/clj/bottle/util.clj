(ns bottle.util
  (:require [taoensso.timbre :as log]))

(defn pretty
  [form]
  (with-out-str (clojure.pprint/pprint form)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn map-vals
  [f coll]
  (into {} (map (fn [[k v]] [k (f v)]) coll)))

(defmacro log-exceptions
  [message & body]
  `(try
     ~@body
     (catch Exception e#
       (log/error e# ~message)
       (throw e#))))
