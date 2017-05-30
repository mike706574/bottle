(ns bottle.server.api.routes
  (:require [compojure.core :as compojure :refer [ANY DELETE GET POST PUT]]
            [compojure.route :as route]
            [bottle.api.event-handler :as event-handler]
            [bottle.api.event-manager :as event-manager]
            [bottle.server.api.websocket :as websocket]
            [bottle.server.http :refer [with-body
                                      handle-exceptions
                                      body-response
                                      not-acceptable
                                      parsed-body
                                      unsupported-media-type]]))

(defn handle-retrieving-events
  [{:keys [event-manager]} request]
  (handle-exceptions request
    (or (unsupported-media-type request)
        (let [response (event-manager/events event-manager)]
          (body-response 200 request response)))))

(defn handle-creating-event
  [{:keys [event-manager]} request]
  (handle-exceptions request
    (with-body [event :bottle/event request]
      (let [response (dosync (event-manager/store event-manager event))]
        (body-response 201 request response)))))

(defn routes
  [deps]
  (compojure/routes
   (GET "/api/events" request
        (handle-retrieving-events deps request))
   (POST "/api/events" request
         (handle-creating-event deps request))
   (route/not-found {:status 200})))
