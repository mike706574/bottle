(ns bottle.server.api.routes
  (:require [bottle.api.event-handler :as handler]
            [bottle.api.event-manager :as event-manager]
            [bottle.server.api.websocket :as websocket]
            [bottle.server.http :refer [with-body
                                        handle-exceptions
                                        body-response
                                        not-acceptable
                                        parsed-body
                                        unsupported-media-type]]
            [compojure.core :as compojure :refer [ANY DELETE GET POST PUT]]
            [compojure.route :as route]
            [taoensso.timbre :as log]))

(defmulti event-clause key)

(defmethod event-clause :type type-clause
  [[_ event-type]]
  (fn [[_ event]]
    (log/info (keyword event-type))
    (log/info (:bottle/event-type event))
    (log/spy :info (= (keyword event-type) (:bottle/event-type event)))))

(defmethod event-clause :default unsupported-param [_] nil)

(defn event-predicate
  [params]
  (apply every-pred (into (list (constantly true))
                          (filter identity (map event-clause params)))))

(defn handle-retrieving-events
  [{:keys [event-manager]} request]
  (handle-exceptions request
    (or (unsupported-media-type request)
        (let [events (event-manager/events event-manager)
              matches? (event-predicate (:params request))
              response (into {} (filter matches? events))]
          (body-response 200 request response)))))

(defn handle-creating-event
  [{:keys [event-handler]} request]
  (handle-exceptions request
    (if-let [event (parsed-body request)]
      (let [{:keys [status event validation-failure]} (handler/handle-event event-handler event)]
        (case status
          :ok (body-response 201 request event)
          :invalid (body-response 400 request validation-failure)
          (body-response 500 request {:bottle.server/message "An error occurred."})))
      {:bottle.server/message "Invalid request body representation."})))

(defn routes
  [deps]
  (compojure/routes
   (GET "/api/events" request
        (handle-retrieving-events deps request))
   (GET "/api/events/:type" request
        (handle-retrieving-events deps request))
   (POST "/api/events" request
         (handle-creating-event deps request))
   (GET "/api/websocket" request (websocket/handler deps))
   (GET "/api/websocket/:event-type" request (websocket/handler deps))
   (route/not-found {:status 200})))
