(ns bottle.server.http
  (:require [bottle.message :as message]
            [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]))

(def primary-media-types #{"application/edn"
                           "application/json"
                           "application/transit+json"
                           "application/transit+msgpack"})

(defn unsupported-media-type?
  [{headers :headers} supported-media-types]
  (if-let [content-type (get headers "content-type")]
    (not (contains? supported-media-types content-type))
    true))

(defn unsupported-media-type
  ([request]
   (unsupported-media-type request primary-media-types))
  ([request supported-media-types]
   (when (unsupported-media-type? request supported-media-types)
     {:status 415
      :headers {"Accepts" supported-media-types}})))

(defn not-acceptable?
  [{headers :headers} supported-media-types]
  (if-let [accept (get headers "accept")]
    (not (contains? supported-media-types accept))
    true))

(defn not-acceptable
  ([request]
   (not-acceptable request primary-media-types))
  ([request supported-media-types]
   (when (not-acceptable? request supported-media-types)
     {:status 406
      :headers {"Consumes" supported-media-types }})))

(defn parsed-body
  [request]
  (let [content-type (get-in request [:headers "content-type"])]
    (try
      (message/decode content-type (:body request))
      (catch Exception ex
        (log/error ex (str "Failed to decode " content-type " request body."))))))

(defn response-body "application/transit+msgpack"
  [request body]
  (let [content-type (get-in request [:headers "accept"])]
    (try
      (message/encode content-type body)
      (catch Exception ex
        (throw (ex-info (str "Failed to write " content-type " response body.")
                        {:request request
                         :body body
                         :exception ex}))))))

(defn body-response
  [status request body]
  {:status status
   :headers {"Content-Type" (get-in request [:headers "accept"])}
   :body (response-body request body)})

(defmacro with-body
  [[body-sym body-spec request] & body]
  `(or (unsupported-media-type ~request)
       (let [~body-sym (parsed-body ~request)]
         (if-not ~body-sym
           (body-response 400 ~request {:bottle.server/message "Invalid request body representation."})
           (if-let [validation-failure# (s/explain-data ~body-spec ~body-sym)]
             (body-response 400 ~request {:bottle.server/message "Invalid request body."
                                          :bottle.server/data validation-failure#})
             (do ~@body))))))

(defmacro handle-exceptions
  [request & body]
  `(try
     ~@body
     (catch Exception e#
       (log/error e# "An exception was thrown while processing a request.")
       (body-response 500 ~request {:bottle.server/message "An error occurred."}))))
