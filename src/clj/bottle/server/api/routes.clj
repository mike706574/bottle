(ns bottle.server.api.routes
  (:require [bottle.event-handler :as event-handler]
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
            [clojure.walk :as walk]
            [clojure.spec.alpha :as s]
            [compojure.core :as compojure :refer [ANY DELETE GET PATCH POST PUT]]
            [compojure.route :as route]
            [taoensso.timbre :as log]))

(defn retrieve-events
  [{:keys [event-manager]} request]
  (handle-exceptions request
    (or (unsupported-media-type request)
        (let [events (event-manager/events event-manager (-> request
                                                             :query-params
                                                             walk/keywordize-keys))]
          (body-response 200 request events)))))

(defn retrieve-categories
  [{:keys [event-manager]} request]
  (handle-exceptions request
    (or (unsupported-media-type request)
        (let [categories (event-manager/categories event-manager)]
          (body-response 200 request categories)))))

(defn create-event
  [{:keys [event-handler]} request]
  (handle-exceptions request
    ;; TODO: Why not use with-body?
    (if-let [event (parsed-body request)]
      (let [{:keys [status event validation-failure]} (event-handler/handle-event event-handler event)]
        (case status
          :ok (body-response 201 request event)
          :invalid (body-response 400 request validation-failure)
          (body-response 500 request {:bottle.server/message "An error occurred."})))
      (body-response 400 request {:bottle.server/message "Invalid request body representation."}))))

(s/def :bottle/operation #{"close"})
(s/def :bottle/patch (s/keys :req [:bottle/operation]))

(defn close-event
  [{:keys [event-handler]} request]
  (handle-exceptions request
    (if-let [id (get-in request [:params :id])]
      (with-body [patch :bottle/patch request]
        (let [operation (:bottle/operation patch)]
          (case operation
            "close" (let [{:keys [status event]} (event-handler/close-event event-handler id)]
                      (case status
                        :ok (body-response 200 request event)
                        :missing (body-response 404 request {:bottle.server/message (str "Event " id " not found.")})
                        (body-response 500 request "An error occurred."))))))
      (body-response 400 {:bottle.server/message "No event ID provided."}))))

(defn get-event
  [{:keys [event-manager]} request]
  (handle-exceptions request
    (if-let [id (get-in request [:params :id])]
      (if-let [event (event-manager/event event-manager id)]
        (body-response 200 request event)
        (body-response 404 request {:bottle.server/message (str "Event " id " not found.")}))
      (body-response 400 {:bottle.server/message "No event ID provided."}))))

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
     (GET "/api/events/:id" request
          (or (unauthenticated request) (get-event deps request)))
     (GET "/api/events" request
          (or (unauthenticated request) (retrieve-events deps request)))
     (POST "/api/events" request
           (or (unauthenticated request) (create-event deps request)))
     (PATCH "/api/events/:id" request
            (or (unauthenticated request) (close-event deps request)))
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
               (log/error e "An exception was thrown while attempting to generate a token.")
               {:status 500
                :headers {"Content-Type" "text/plain"}
                :body "An error occurred."})))
     (GET "/api/websocket" []
          (websocket/handler deps))
     (GET "/api/websocket/:category" []
          (websocket/handler deps))
     ;; TODO: Return proper accept type?
     (route/not-found {:status 404}))))
