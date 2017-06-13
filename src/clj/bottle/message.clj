(ns bottle.message
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [cognitect.transit :as transit]))

(def supported-content-type #{"application/edn"
                              "application/json"
                              "application/transit+json"
                              "application/transit+msgpack"})

(def ^:private byte-array-type (type (byte-array [])))
(def byte-array? (partial instance? byte-array-type))
(def input-stream? (partial instance? java.io.InputStream))

(s/fdef decode
  :args (s/cat :content-type supported-content-type
               :body (s/or :string string?
                           :byte-array byte-array?
                           :input-stream input-stream?))
  :ret any?)

(s/fdef encode
  :args (s/cat :content-type supported-content-type
               :body any?)
  :ret byte-array?)

(defmulti ^:private decode-stream
  "Encodes the string using the given content-type."
  (fn [content-type body] content-type))

(defmethod decode-stream "application/edn"
  [_ body]
  (-> body slurp edn/read-string))

(defmethod decode-stream "application/transit+json"
  [_ body]
  (transit/read (transit/reader body :json)))

(defmethod decode-stream "application/transit+msgpack"
  [_ body]
  (transit/read (transit/reader body :msgpack)))

(defmethod decode-stream "application/json"
  [_ body]
  (json/read (io/reader body) :key-fn keyword))

(defmethod decode-stream :default
  [content-type body]
  (throw (ex-info (str "Content type \"" content-type "\" is not supported.")
                  {:body body
                   :content-type content-type})))

(defn ^:private decode-string
  [content-type body]
  (decode-stream content-type ))

(defn ^:private decode-bytes
  [content-type body]
  (decode-stream content-type ))

(defn decode
  [content-type body]
  (decode-stream
   content-type
   (condp instance? body
     java.io.InputStream body
     String (if (= content-type "application/transit+msgpack")
              (throw (ex-info "Strings are not supported when using application/transit+msgpack."
                              {:content-type content-type
                               :body body}))
              (java.io.ByteArrayInputStream. (.getBytes body)))
     byte-array-type (java.io.ByteArrayInputStream. body)
     (throw (ex-info (str "Body type \"" (type body) "\" is not supported.")
                     {:content-type content-type
                      :body body})))))

(defmulti encode
  "Encodes the string using the given content-type."
  (fn [content-type body] content-type))

(defmethod encode "application/json"
  [_ body]
  (.getBytes (json/write-str body)))

(defmethod encode "application/edn"
  [_ body]
  (.getBytes (pr-str body)))

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

(defmethod encode :default
  [content-type body]
  (throw (ex-info (str "Content type \"" content-type "\" is not supported.")
                  {:content-type content-type
                   :body body})))
