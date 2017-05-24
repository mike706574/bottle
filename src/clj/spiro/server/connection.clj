(ns spiro.server.connection
  (:require [com.stuartsierra.component :as component]
            [manifold.stream :as s]
            [spiro.server.util :as util]
            [taoensso.timbre :as log]))

(defprotocol ConnectionManager
  "Manages connections."
  (add! [this type conn] "Add a connection.")
  (close-all! [this] "Closes all connections."))

(defrecord AtomConnectionManager [counter connections]
  ConnectionManager
  (add! [this type conn]
    (let [conn-id (swap! counter inc)]
      (swap! connections assoc conn-id {:id conn-id
                                        :type type
                                        :conn conn})
      conn-id))
  (close-all! [this]
    (let [all-conns (flatten (vals @connections))
          conn-count (count all-conns)]
      (when (pos? conn-count)
        (log/debug (str "Closing " conn-count " connections."))
        (doseq [entry all-conns]
          (s/close! (:conn entry)))))))

(defn manager
  []
  (component/using (map->AtomConnectionManager {:counter (atom 0)})
    [:connections]))
