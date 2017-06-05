(ns bottle.util)

(defn pretty
  [form]
  (with-out-str (clojure.pprint/pprint form)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn map-vals
  [f coll]
  (into {} (map (fn [[k v]] [k (f v)]) coll)))

(defn test-array
    [t]
    (let [check (type (t []))]
          (fn [arg] (instance? check arg))))

(def ^:private byte-array-type (type (byte-array [])))
(def byte-array? (partial instance? byte-array-type))
