(ns bottle.postgresql
  (:require [boomerang.json :as json]
            [clojure.java.jdbc :as jdbc])
  (:import org.postgresql.util.PGobject))

(defn ^:private to-pgjson
  [value]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (json/write-str value))))

(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentMap
  (sql-value [value] (to-pgjson value))
  clojure.lang.IPersistentVector
  (sql-value [value] (to-pgjson value)))

(extend-protocol jdbc/IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj _metadata _index]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (json/read-str value))))
