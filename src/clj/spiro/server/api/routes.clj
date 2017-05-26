(ns spiro.server.api.routes
  (:require [clojure.spec.alpha :as s]
            [compojure.core :as compojure :refer [ANY DELETE GET POST PUT]]
            [compojure.route :as route]
            [spiro.api.event-manager :as event-manager]
            [spiro.server.api.websocket :as websocket]
            [spiro.server.http :refer [with-body
                                      handle-exceptions
                                      body-response
                                      not-acceptable
                                      parsed-body
                                      unsupported-media-type]]))

(s/def :spiro/event-id keyword?)
(s/def :spiro/event-type keyword?)
(s/def :spiro/event (s/keys :req [:spiro/event-type]))

(defn handle-retrieving-events
  [{:keys [event-manager]} request]
  (handle-exceptions request
    (or (unsupported-media-type request)
        (let [response (event-manager/events event-manager)]
          (body-response 200 request response)))))

(defn handle-creating-event
  [{:keys [event-manager]} request]
  (handle-exceptions request
    (with-body [event :spiro/event request]
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
