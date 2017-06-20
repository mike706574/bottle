(ns bottle.server.system-test
  (:require [aleph.http :as http]
            [bottle.client :as client]
            [bottle.macros :refer [with-system]]
            [bottle.message :as message]
            [bottle.messaging.producer :as producer]
            [bottle.server.system :as system]
            [bottle.util :as util :refer [map-vals]]
            [com.stuartsierra.component :as component]
            [clojure.test :refer [deftest testing is]]
            [manifold.bus :as bus]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [taoensso.timbre :as log]))

(def event-messaging {:bottle/broker-type :rabbit-mq
                      :bottle/broker-path "localhost"
                      :bottle/queue-name "bottle-system-test"})

(def port 9001)
(def content-type "application/transit+json")
(def config {:bottle/id "bottle-server"
             :bottle/port port
             :bottle/log-path "/tmp"
             :bottle/user-manager-type :atomic
             :bottle/event-content-type content-type
             :bottle/event-messaging event-messaging
             :bottle/users {"mike" "rocket"}})

(defmacro unpack-response
  [call & body]
  `(let [~'response ~call
         ~'status (:status ~'response)
         ~'body (:body ~'response)
         ~'text (util/pretty ~'response)]
     ~@body))

(defn purge [event]
  (if (map? event)
    (dissoc event
            :bottle/id
            :bottle/time)
    event))

(deftest simple-test
  (with-system (system/system config)
    (let [client (-> {:url (str "http://localhost:" port)
                      :content-type content-type}
                     (client/client)
                     (client/authenticate {:bottle/username "mike"
                                           :bottle/password "rocket"}))
          foo-1 {:bottle/category :foo
                 :bottle/closed? false
                 :count 4 }]
      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= {} (map-vals purge body))))

      ;; create
      (unpack-response (client/create-event client {:bottle/category :foo :count 4})
        (is (= 201 status))
        (is (string? (:bottle/id body)))
        (is (instance? java.util.Date (:bottle/time body)))
        (is (= foo-1 (purge body))))

      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= {"1" foo-1} (map-vals purge body)))))))

(deftest messaging
  (with-system (system/system config)
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
                 :count 4}
          producer (producer/producer event-messaging)]

      (s/consume #(reset! last-event %) (bus/subscribe bus :all))
      (s/consume #(reset! last-foo %) (bus/subscribe bus :foo))

      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= {} body)))

      (producer/produce producer (String. (message/encode "application/transit+json" {:bottle/category :foo :count 4}) "UTF-8"))

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

(deftest creating-and-querying-events
  (with-system (system/system config)
    (let [host (str "localhost:" port)
          http-url (str "http://" host)
          ws-url (str "ws://" host)
          client (-> {:url http-url
                      :content-type content-type}
                     (client/client)
                     (client/authenticate {:bottle/username "mike"
                                           :bottle/password "rocket"}))
          all-conn (client/connect! ws-url)
          foo-conn (client/connect! ws-url :foo)
          bar-conn (client/connect! ws-url :bar)
          baz-conn (client/connect! ws-url :baz)
          bus (:event-bus system)
          last-event (atom nil)
          last-foo (atom nil)
          last-bar (atom nil)
          foo-1 {:bottle/category :foo
                 :bottle/closed? false
                 :count 4 }
          bar-2 {:bottle/category :bar
                 :bottle/closed? false
                 :name "Bob"}
          foo-3 {:bottle/category :foo
                 :bottle/closed? false
                 :count 15}
          baz-4 {:bottle/category :baz
                 :bottle/closed? false
                 :numbers [1 2 3]}
          {:keys [bottle/event-messaging bottle/event-content-type]} config
          producer (producer/producer event-messaging)]

      (s/consume #(reset! last-event %) (bus/subscribe bus :all))
      (s/consume #(reset! last-foo %) (bus/subscribe bus :foo))
      (s/consume #(reset! last-bar %) (bus/subscribe bus :bar))

      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= {} (map-vals purge body))))

      ;; create
      (unpack-response (client/create-event client {:bottle/category :foo :count 4})
        (is (= 201 status))
        (is (string? (:bottle/id body)))
        (is (instance? java.util.Date (:bottle/time body)))
        (is (= foo-1 (purge body))))

      ;; websockets
      (is (= foo-1 (purge (client/receive! all-conn))))
      (is (= foo-1 (purge (client/receive! foo-conn))))
      (is (= :timeout (purge (client/receive! bar-conn))))

      ;; bus
      (is (= foo-1 (purge @last-event)))
      (is (= foo-1 (purge @last-foo)))
      (is (nil? @last-bar))

      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= {"1" foo-1} (map-vals purge body))))
      (unpack-response (client/events-by-category client :bar)
        (is (= 200 status))
        (is (= {} body)))
      (unpack-response (client/events-by-category client :foo)
        (is (= 200 status))
        (is (= {"1" foo-1} (map-vals purge body))))

      ;; create
      (unpack-response (client/create-event client {:bottle/category :bar :name "Bob"})
        (is (= 201 status))
        (is (= bar-2 (purge body))))

      ;; websockets
      (is (= bar-2 (purge (client/receive! all-conn))))
      (is (= :timeout (purge (client/receive! foo-conn))))
      (is (= bar-2 (purge (client/receive! bar-conn))))

      ;; bus
      (is (= bar-2 (purge @last-event)))
      (is (= foo-1 (purge @last-foo)))
      (is (= bar-2 (purge @last-bar)))

      ;; create
      (unpack-response (client/create-event client {:bottle/category :foo :count 15})
        (is (= 201 status))
        (is (= foo-3 (purge body))))

      ;; websockets
      (is (= foo-3 (purge (client/receive! all-conn))))
      (is (= foo-3 (purge (client/receive! foo-conn))))
      (is (= :timeout (purge (client/receive! bar-conn))))

      ;; bus
      (is (= foo-3 (purge @last-event)))
      (is (= foo-3 (purge @last-foo)))
      (is (= bar-2 (purge @last-bar)))

      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= {"1" foo-1  "2" bar-2 "3" foo-3} (map-vals purge body))))
      (unpack-response (client/events-by-category client :bar)
        (is (= 200 status))
        (is (= {"2" bar-2} (map-vals purge body))))
      (unpack-response (client/events-by-category client :foo)
        (is (= 200 status))
        (is (= {"1" foo-1 "3" foo-3} (map-vals purge body))))

      ;; create via message
      (producer/produce producer (String. (message/encode "application/transit+json" baz-4) "UTF-8"))

      (Thread/sleep 1000)

      ;; websockets
      (is (= baz-4 (purge (client/receive! all-conn))))
      (is (= :timeout (purge (client/receive! foo-conn))))
      (is (= :timeout (purge (client/receive! bar-conn))))
      (is (= baz-4 (purge (client/receive! baz-conn))))

      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= {"1" foo-1  "2" bar-2 "3" foo-3 "4" baz-4} (map-vals purge body))))
      (unpack-response (client/events-by-category client :bar)
        (is (= 200 status))
        (is (= {"2" bar-2} (map-vals purge body))))
      (unpack-response (client/events-by-category client :foo)
        (is (= 200 status))
        (is (= {"1" foo-1 "3" foo-3} (map-vals purge body))))
      (unpack-response (client/events-by-category client :baz)
        (is (= 200 status))
        (is (= {"4" baz-4} (map-vals purge body)))))))
