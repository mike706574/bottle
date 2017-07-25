(ns bottle.event-manager.query
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as str]))

(defn equality-clause
  [key]
  (fn prop [options]
    (let [value (get options key)]
      (when-not (nil? value)
        [::prop-equality key value]))))

(def supported-clauses [(equality-clause :bottle/category)
                        (equality-clause :bottle/closed?)])

(defn clauses
  [options]
  (->> supported-clauses
       (map #(% options))
       (filter identity)))

(defn xform [clauses]
  (letfn [(xf [clause]
            (match clause
              [::prop-equality k v] (do (println k "===" v) (filter #(= (get % k) v)))))]
    (apply comp (map xf clauses))))

(defn where [clauses]
  (letfn [(part [clause]
            (match clause
              [::prop-equality k v] (str k " = " v)))]
    (str/join "," (map part clauses))))
