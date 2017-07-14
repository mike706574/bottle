(ns bottle.server.api.routes
  (:require [bottle.event-handler :as handler]
            [bottle.event-manager :as event-manager]
            [bottle.users :as users]
            [bottle.server.api.websocket :as websocket]
            [bottle.server.authentication :as auth]
            [boomerang.http :refer [with-body
                                    handle-exceptions
                                    body-response
                                    not-acceptable
                                    parsed-body
                                    unsupported-media-type]]
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

(defn retrieve-categories
  [{:keys [event-manager]} request]
  (handle-exceptions request
    (or (unsupported-media-type request)
        (let [categories (event-manager/categories event-manager)]
          (body-response 200 request categories)))))

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

(defn routes
  [{:keys [user-manager authenticator] :as deps}]
  (letfn [(unauthenticated [request]
            (when-not (auth/authenticated? authenticator request)
              {:status 401}))]
    (compojure/routes
     (GET "/api/healthcheck" request
          {:status 200})
     (GET "/api/categories" request
          (or (unauthenticated request) (retrieve-categories deps request)))
     (GET "/api/events" request
          (or (unauthenticated request) (retrieve-events deps request)))
     (GET "/api/events/:category" request
          (or (unauthenticated request) (retrieve-events deps request)))
     (POST "/api/events" request
           (or (unauthenticated request) (create-event deps request)))
     (POST "/api/tokens" request
           (try
             (or (not-acceptable request #{"text/plain"})
                 (with-body [credentials :bottle/credentials request]
                   (if-let [user (users/authenticate user-manager credentials)]
                     {:status 201
                      :headers {"Content-Type" "text/plain"}
                      :body (auth/token authenticator (:bottle/username credentials))}
                     {:status 401})))
             (catch Exception e
               (log/error e "An exception was thrown while processing a request.")
               {:status 500
                :headers {"Content-Type" "text/plain"}
                :body "An error occurred."})))
     (GET "/api/websocket" []
          (websocket/handler deps))
     (GET "/api/websocket/:category" []
          (websocket/handler deps))
     ;; TODO: Return proper accept type?
     (route/not-found {:status 404}))))
