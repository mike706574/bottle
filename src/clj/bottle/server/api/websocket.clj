(ns bottle.server.api.websocket
  (:require [aleph.http :as http]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [manifold.bus :as bus]
            [bottle.server.connection :as conn]
            [bottle.message :as message]
            [taoensso.timbre :as log]))

(defn non-websocket-response
  []
  {:status 400
   :headers {"content-type" "application/text"}
   :body "Expected a websocket request."})

(defn handle
  [{:keys [event-bus conn-manager] :as deps} req]
  (d/let-flow [conn (d/catch
                        (http/websocket-connection req)
                        (constantly nil))]
    (if-not conn
      (non-websocket-response)
      (let [conn-id (conn/add! conn-manager :menu conn)
            conn-label (str "[ws-conn-" conn-id "] ")]
        (log/debug (str conn-label "Initial connection established."))
        (try
          (s/connect-via
           (bus/subscribe event-bus :all)
           (fn [message]
             (s/put! conn (message/encode "application/transit+json" message)))
           conn)
          {:status 101}
          (catch Exception e
            (log/error e (str conn-label "Exception thrown while setting up connection."))
            {:status 500}))))))

(defn handler
  [deps]
  (partial handle deps))
