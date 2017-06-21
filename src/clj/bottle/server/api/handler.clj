(ns bottle.server.api.handler
  (:require [bottle.server.api.routes :as api-routes]
            [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [clojure.string :as str]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults
                                              api-defaults]]
            [taoensso.timbre :as log]))

(defn wrap-logging
  [handler]
  (fn [{:keys [uri request-method] :as request}]
    (let [label (str (-> request-method name str/upper-case) " \"" uri "\"")]
      (try
        (log/debug label)
        (let [{:keys [status] :as response} (handler request)]
          (log/debug (str label " -> " status))
          response)
        (catch Exception e
          (log/error e label)
          {:status 500})))))

(defn handler
  [deps]
  (let [secret-key (:secret-key deps)
        auth-backend (jws-backend {:secret secret-key :options {:alg :hs512}})]
    (-> (api-routes/routes deps)
        (wrap-cors :access-control-allow-origin [#".*"]
                   :access-control-allow-methods [:get :put :post :delete])
        (wrap-authorization auth-backend)
        (wrap-authentication auth-backend)
        (wrap-defaults api-defaults)
        (wrap-logging))))
