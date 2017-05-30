(ns bottle.message
  (:require [clojure.edn :as edn]
            [cognitect.transit :as transit]
            [taoensso.timbre :as log]))

(defmulti decode-stream (fn [content-type body] content-type))

(defmethod decode-stream "application/edn"
  [_ body]
  (-> body slurp edn/read-string))

(defmethod decode-stream "application/transit+json"
  [_ body]
  (transit/read (transit/reader body :json)))

(defmethod decode-stream "application/transit+msgpack"
  [_ body]
  (transit/read (transit/reader body :msgpack)))

(defn decode-string
  [content-type body]
  (decode-stream content-type (java.io.ByteArrayInputStream. (.getBytes body))))

(defn decode-bytes
  [content-type body]
  (decode-stream content-type (java.io.ByteArrayInputStream. body)))

(defn decode
  [content-type body]
  ((cond
     (instance? java.io.InputStream body) decode-stream
     (string? body) decode-string
     :else decode-bytes)
   content-type body))

(defmulti encode (fn [content-type body] content-type))
(defmethod encode "application/edn"
  [_ body]
  (pr-str body))

(defmethod encode "application/transit+json"
  [_ body]
  (let [out (java.io.ByteArrayOutputStream.)]
    (transit/write (transit/writer out :json) body)
    (.toByteArray out)))

(defmethod encode "application/transit+msgpack"
  [_ body]
  (let [out (java.io.ByteArrayOutputStream.)]
    (transit/write (transit/writer out :msgpack) body)
    (.toByteArray out)))
