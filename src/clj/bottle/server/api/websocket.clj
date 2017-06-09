(ns bottle.server.api.websocket
  (:require [aleph.http :as http]
            [bottle.util :as util]
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
  [{:keys [event-content-type event-bus conn-manager] :as deps} req]
  (let [category (or (keyword (get-in req [:params :category])) :all)]
    (d/let-flow [conn (d/catch
                          (http/websocket-connection req)
                          (constantly nil))]
      (if-not conn
        (non-websocket-response)
        (let [conn-id (conn/add! conn-manager :menu conn)
              conn-label (str "[ws-conn-" conn-id "] ")]
          (log/debug (str conn-label "Websocket connection established for event category " category "."))
          (try
            (s/connect-via
             (bus/subscribe event-bus category)
             (fn [event]
               (util/log-exceptions
                (str conn-label "Failed to send event:\n" (util/pretty event))
                (s/put! conn (message/encode event-content-type event))))
             conn)
            {:status 101}
            (catch Exception e
              (log/error e (str conn-label "Exception thrown while setting up connection."))
              {:status 500})))))))

(defn handler [deps] (partial handle deps))
