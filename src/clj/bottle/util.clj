(ns bottle.util)

(defn pretty
  [form]
  (with-out-str (clojure.pprint/pprint form)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn separate
  [f coll]
  (loop [[head & tail] coll
         out (vector (list) (list))]
    (if (nil? head)
      out
      (recur tail (update out (if (f head) 0 1) conj head)))))

(defn map-vals
  [f coll]
  (into {} (map (fn [[k v]] [k (f v)]) coll)))
