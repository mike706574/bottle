(ns bottle.server.api.routes
  (:require [bottle.event-handler :as handler]
            [bottle.event-manager :as event-manager]
            [bottle.users :as users]
            [bottle.server.api.websocket :as websocket]
            [bottle.server.http :refer [with-body
                                        handle-exceptions
                                        body-response
                                        not-acceptable
                                        parsed-body
                                        unsupported-media-type]]
            [buddy.auth :refer [authenticated?]]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]
            [compojure.core :as compojure :refer [ANY DELETE GET PATCH POST PUT]]
            [compojure.route :as route]
            [taoensso.timbre :as log]))

(defmulti event-clause key)

(defmethod event-clause :category category-clause
  [[_ category]]
  (fn [[_ event]]
    (= (keyword category) (:bottle/category event))))

(defmethod event-clause :default unsupported-param [_] nil)

(defn event-predicate
  [params]
  (apply every-pred (into (list (constantly true))
                          (filter identity (map event-clause params)))))

(defn retrieve-events
  [{:keys [event-manager]} request]
  (handle-exceptions request
    (or (unsupported-media-type request)
        (let [events (event-manager/events event-manager)
              matches? (event-predicate (:params request))
              response (into {} (filter matches? events))]
          (body-response 200 request response)))))

(defn create-event
  [{:keys [event-handler]} request]
  (handle-exceptions request
    (if-let [event (parsed-body request)]
      (let [{:keys [status event validation-failure]} (handler/handle-event event-handler event)]
        (case status
          :ok (body-response 201 request event)
          :invalid (body-response 400 request validation-failure)
          (body-response 500 request {:bottle.server/message "An error occurred."})))
      {:bottle.server/message "Invalid request body representation."})))

(defn unauthorized
  [request]
  (when-not (authenticated? request)
    {:status 401}))

(defn routes
  [{:keys [user-manager secret-key] :as deps}]
  (compojure/routes
   (GET "/api/healthcheck" request
        {:status 200})
   (GET "/api/events" request
        (or (unauthorized request) (retrieve-events deps request)))
   (GET "/api/events/:category" request
        (or (unauthorized request) (retrieve-events deps request)))
   (POST "/api/events" request
         (or (unauthorized request) (create-event deps request)))
   (POST "/api/tokens" request
         (with-body [credentials :bottle/credentials request]
           (if-let [user (users/authenticate user-manager credentials)]
             {:status 201
              :headers {"Content-Type" "text/plain"}
              :body (let [claims {:username (:bottle/username credentials)
                                  :exp (time/plus (time/now) (time/days 1))}]
                      (jwt/sign claims secret-key {:alg :hs512}))}
             {:status 401})))
   (GET "/api/websocket" request (websocket/handler deps))
   (GET "/api/websocket/:category" request (websocket/handler deps))
   (route/not-found {:status 404})))
