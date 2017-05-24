(ns spiro.server.api.routes
  (:require [compojure.core :as compojure :refer [ANY DELETE GET POST PUT]]
            [compojure.route :as route]
            [spiro.server.api.websocket :as websocket]
            [spiro.server.http :refer [with-body
                                      handle-exceptions
                                      body-response
                                      not-acceptable
                                      parsed-body
                                      unsupported-media-type]]))

(defn routes
  [deps]
  (compojure/routes
   (GET "/api/hello" request
        (or (not-acceptable request)
            (unsupported-media-type request)
            (body-response 200 request {:message "Hello!"})))
   (route/not-found {:status 200})))
