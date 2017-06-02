(ns bottle.server.system-test
  (:require [aleph.http :as http]
            [bottle.util :as util]
            [com.stuartsierra.component :as component]
            [clojure.test :refer [deftest testing is]]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [bottle.server.system :as system]
            [bottle.message :as message]
            [taoensso.timbre :as log]))

(def content-type "application/transit+json")

(def config {:bottle/id "bottle-server"
             :bottle/port 9000
             :bottle/log-path "/tmp"
             :bottle/event-content-type "application/transit+json"
             :bottle/event-messaging {:bottle/broker-type :rabbit-mq
                                      :bottle/broker-path "localhost"
                                      :bottle/queue-name "bottle-1"}})

(defmacro with-system
  [& body]
  (let [port (:bottle/port config)
        ws-url (str "ws://localhost:" port "/api/websocket")]
    `(let [~'system (component/start-system (system/system config))
           ~'ws-url ~ws-url
           ~'http-url (str "http://localhost:" ~port)]
       (try
         ~@body
         (finally (component/stop-system ~'system))))))

(defmacro unpack-response
  [call & body]
  `(let [~'response ~call
         ~'status (:status ~'response)
         ~'body (:body ~'response)
         ~'text (util/pretty ~'response)]
     ~@body))



;; ws client (unused)
(defn receive!
  [conn]
  (let [out @(s/try-take! conn :drained 2000 :timeout)]
    (if (contains? #{:drained :timeout} out) out (message/decode content-type out))))

(defn flush!
  [conn]
  (loop [out :continue]
    (when (not= out :timeout)
      (recur @(s/try-take! conn :drained 10 :timeout)))))

(defn send!
  [conn message]
  (s/put! conn (message/encode content-type message)))

(defn parse
  [request]
  (if (contains? request :body)
    (update request :body (comp (partial message/decode content-type)))
    request))

(defn connect!
  [ws-url]
  (let [conn @(http/websocket-client ws-url)]
    (is (not= :timeout (receive! conn)))
    conn))

;; http client
(defn transit-get
  [url]
  (parse @(http/get url
                    {:headers {"Content-Type" content-type
                               "Accept" content-type}
                     :throw-exceptions false})))

(defn transit-post
  [url body]
  (parse @(http/post url
                     {:headers {"Content-Type" content-type
                                "Accept" content-type}
                      :body (message/encode content-type body)
                      :throw-exceptions false})))

(defn get-events
  [http-url]
  (transit-get (str http-url "/api/events")))

(defn get-events-by-type
  [http-url event-type]
  (parse @(http/get (str http-url "/api/events")
                    {:headers {"Content-Type" content-type
                               "Accept" content-type}
                     :query-params {"type" (name event-type)}
                     :throw-exceptions false})))

(defn create-event
  [http-url event]
  (transit-post (str http-url "/api/events") event))

;; test
(deftest creating-and-querying-events
  (with-system
    (let [foo-1 {:bottle/event-type :foo
                 :bottle/event-id "1"
                 :count 4}
          bar-2 {:bottle/event-type :bar
                 :bottle/event-id "2"
                 :name "Bob"}
          foo-3 {:bottle/event-type :foo
                 :bottle/event-id "3"
                 :count 15}]
      ;; query
      (unpack-response (get-events http-url)
        (is (= 200 status))
        (is (= {} body)))

      ;; create
      (unpack-response (create-event http-url {:bottle/event-type :foo :count 4})
        (is (= 201 status))
        (is (= foo-1 body) text))

      ;; query
      (unpack-response (get-events http-url)
        (is (= 200 status))
        (is (= {"1" foo-1} body)))
      (unpack-response (get-events-by-type http-url :bar)
        (is (= 200 status))
        (is (= {} body)))
      (unpack-response (get-events-by-type http-url :foo)
        (is (= 200 status))
        (is (= {"1" foo-1} body)))

      ;; create
      (unpack-response (create-event http-url {:bottle/event-type :bar :name "Bob"})
        (is (= 201 status))
        (is (= bar-2 body) text))
      (unpack-response (create-event http-url {:bottle/event-type :foo :count 15})
        (is (= 201 status))
        (is (= foo-3 body) text))

      ;; query
      (unpack-response (get-events http-url)
        (is (= 200 status))
        (is (= {"1" foo-1  "2" bar-2 "3" foo-3} body)))
      (unpack-response (get-events-by-type http-url :bar)
        (is (= 200 status))
        (is (= {"2" bar-2} body)))
      (unpack-response (get-events-by-type http-url :foo)
        (is (= 200 status))
        (is (= {"1" foo-1 "3" foo-3} body))))))
