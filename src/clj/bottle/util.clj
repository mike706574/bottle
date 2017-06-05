(ns bottle.util)

(defn pretty
  [form]
  (with-out-str (clojure.pprint/pprint form)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn map-vals
  [f coll]
  (into {} (map (fn [[k v]] [k (f v)]) coll)))
