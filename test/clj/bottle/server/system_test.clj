(ns bottle.server.system-test
  (:require [aleph.http :as http]
            [bottle.client :as client]
            [bottle.util :as util :refer [map-vals]]
            [com.stuartsierra.component :as component]
            [clojure.test :refer [deftest testing is]]
            [manifold.bus :as bus]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [bottle.server.system :as system]
            [bottle.message :as message]
            [taoensso.timbre :as log]))

(def config {:bottle/id "bottle-server"
             :bottle/port 9001
             :bottle/log-path "/tmp"
             :bottle/event-content-type "application/transit+json"
             :bottle/event-messaging {:bottle/broker-type :active-mq
                                      :bottle/broker-path "tcp://qdsdevamq.qg.com:61616"
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

(defn purge [event]
  (dissoc event :bottle/event-id :bottle/event-time))

;; test
(deftest creating-and-querying-events
  (with-system
    (let [all-conn (client/connect! ws-url)
          bus (:event-bus system)
          last-event (atom nil)
          last-foo (atom nil)
          last-bar (atom nil)
          foo-1 {:bottle/event-type :foo :count 4}
          bar-2 {:bottle/event-type :bar :name "Bob"}
          foo-3 {:bottle/event-type :foo :count 15}]

      (s/consume #(reset! last-event %) (bus/subscribe bus :all))
      (s/consume #(reset! last-foo %) (bus/subscribe bus :foo))
      (s/consume #(reset! last-bar %) (bus/subscribe bus :bar))

      ;; query
      (unpack-response (client/get-events http-url)
        (is (= 200 status))
        (is (= {} (map-vals purge body))))

      ;; create
      (unpack-response (client/create-event http-url {:bottle/event-type :foo :count 4})
        (is (= 201 status))
        (is (string? (:bottle/event-id body)))
        (is (instance? java.util.Date (:bottle/event-time body)))
        (is (= foo-1 (purge body))))

      (println "FOO FOO" (client/receive! all-conn))

      ;; bus
      (is (= foo-1 (purge @last-event)))
      (is (= foo-1 (purge @last-foo)))
      (is (nil? @last-bar))

      ;; query
      (unpack-response (client/get-events http-url)
        (is (= 200 status))
        (is (= {"1" foo-1} (map-vals purge body))))
      (unpack-response (client/get-events-by-type http-url :bar)
        (is (= 200 status))
        (is (= {} body)))
      (unpack-response (client/get-events-by-type http-url :foo)
        (is (= 200 status))
        (is (= {"1" foo-1} (map-vals purge body))))

      ;; create
      (unpack-response (client/create-event http-url {:bottle/event-type :bar :name "Bob"})
        (is (= 201 status))
        (is (= bar-2 (purge body))))

      ;; bus
      (is (= bar-2 (purge @last-event)))
      (is (= foo-1 (purge @last-foo)))
      (is (= bar-2 (purge @last-bar)))

      ;; create
      (unpack-response (client/create-event http-url {:bottle/event-type :foo :count 15})
        (is (= 201 status))
        (is (= foo-3 (purge body))))

      ;; bus
      (is (= foo-3 (purge @last-event)))
      (is (= foo-3 (purge @last-foo)))
      (is (= bar-2 (purge @last-bar)))

      ;; query
      (unpack-response (client/get-events http-url)
        (is (= 200 status))
        (is (= {"1" foo-1  "2" bar-2 "3" foo-3} (map-vals purge body))))
      (unpack-response (client/get-events-by-type http-url :bar)
        (is (= 200 status))
        (is (= {"2" bar-2} (map-vals purge body))))
      (unpack-response (client/get-events-by-type http-url :foo)
        (is (= 200 status))
        (is (= {"1" foo-1 "3" foo-3} (map-vals purge body)))))))
