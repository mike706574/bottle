(ns bottle.server.messaging-test
  (:require [aleph.http :as http]
            [bottle.client :as client]
            [bottle.messaging.producer :as producer]
            [bottle.util :as util]
            [com.stuartsierra.component :as component]
            [clojure.test :refer [deftest testing is]]
            [manifold.bus :as bus]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [bottle.server.system :as system]
            [bottle.message :as message]
            [taoensso.timbre :as log]))

(def content-type "application/transit+json")

(def messaging-config {:bottle/broker-type :rabbit-mq
                       :bottle/broker-path "localhost"
                       :bottle/queue-name "bottle-messaging-test"})

(def port 9004)
(def http-url (str "http://localhost:" port))

(def username "mike")
(def password "rocket")
(def credentials {:bottle/username username
                  :bottle/password password})

(def config {:bottle/id "bottle-server"
             :bottle/port port
             :bottle/log-path "/tmp"
             :bottle/event-content-type content-type
             :bottle/event-messaging messaging-config
             :bottle/users {"mike" "rocket" }})

(defn send-message
  [message]
  (producer/produce
   (producer/producer messaging-config)
   (String. (message/encode content-type message) "UTF-8")))

(message/encode "application/transit+msgpack" "foo")

(defmacro with-system
  [& body]
  (let [port (:bottle/port config)
        ws-url (str "ws://localhost:" port "/api/websocket")]
    `(let [~'system (component/start-system (system/system config))
           ~'ws-url ~ws-url
           ~'http-url (str "http://localhost:" ~port)]
       (try
         ~@body
         (finally
           (component/stop-system ~'system))))))

(defmacro unpack-response
  [call & body]
  `(let [~'response ~call
         ~'status (:status ~'response)
         ~'body (:body ~'response)
         ~'text (util/pretty ~'response)]
     ~@body))

(deftest creating-and-querying-events
  (with-system
    (let [client (-> {:url (str "http://localhost:" port)
                      :content-type content-type}
                     (client/client)
                     (client/authenticate {:bottle/username "mike"
                                           :bottle/password "rocket"}))
          bus (:event-bus system)
          last-event (atom nil)
          last-foo (atom nil)
          foo-1 {:bottle/category :foo
                 :bottle/id "1"
                 :count 4}]

      (s/consume #(reset! last-event %) (bus/subscribe bus :all))
      (s/consume #(reset! last-foo %) (bus/subscribe bus :foo))

      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= {} body)))

      (send-message {:bottle/category :foo :count 4})

      (Thread/sleep 100)

      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= ["1"] (keys body)))
        (let [event (get body "1")]
          (is (= #{:bottle/id
                   :bottle/category
                   :bottle/time
                   :bottle/closed?
                   :count}
                 (set (keys event))))
          (is (string? (:bottle/id event)))
          (is (not (:bottle/closed? event)))
          (is (instance? java.util.Date (:bottle/time event)))
          (is (= :foo (:bottle/category event)))
          (is (= 4 (:count event))))))))
