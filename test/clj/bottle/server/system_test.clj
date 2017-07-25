(ns bottle.server.system-test
  (:require [aleph.http :as http]
            [bottle.client :as client]
            [bottle.macros :refer [with-system]]
            [bottle.sanitation :refer [purge purge-all purge-message]]
            [boomerang.message :as message]
            [bottle.messaging.producer :as producer]
            [bottle.server.system :as system]
            [bottle.util :as util :refer [map-vals]]
            [com.stuartsierra.component :as component]
            [clojure.test :refer [deftest testing is]]
            [manifold.bus :as bus]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [taoensso.timbre :as log]))

(def event-messaging {:bottle.messaging/broker-type :stream
                      :bottle.messaging/stream :event
                      :bottle.messaging/broker-path "localhost"
                      :bottle.messaging/queue-name "bottle-system-test"})

(def port 9001)
(def content-type "application/transit+json")
(def config {:bottle/id "bottle-server"
             :bottle/port port
             :bottle/log-path "/tmp"
             :bottle/user-manager-type :atomic
             :bottle/event-manager-type :ref
             :bottle/event-content-type content-type
             :bottle/event-messaging event-messaging
             :bottle/streams [:event]
             :bottle/users {"mike" "rocket"}})

(defmacro unpack-response
  [call & body]
  `(let [~'response ~call
         ~'status (:status ~'response)
         ~'body (:body ~'response)
         ~'text (util/pretty ~'response)]
     ~@body))

(deftest simple-test
  (with-system (system/system config)
    (let [client (-> {:host (str "localhost:" port)
                      :content-type content-type}
                     (client/client)
                     (client/authenticate {:bottle/username "mike"
                                           :bottle/password "rocket"}))
          foo-1 {:bottle/category "foo"                 :bottle/closed? false
                 :count 4 }]
      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= {} (map-vals purge body))))

      ;; create
      (unpack-response (client/create-event client {:bottle/category "foo" :count 4})
        (is (= 201 status))
        (is (string? (:bottle/id body)))
        (is (instance? java.util.Date (:bottle/time body)))
        (is (= foo-1 (purge body)))

        (let [id (:bottle/id body)]
          (unpack-response (client/events client)
            (is (= 200 status))
            (is (= #{foo-1} (purge-all body)))
            (is (= id (:bottle/id (first body))))))))))

(defn producer
  [{stream-manager :stream-manager :as system}]
  (assoc (producer/producer event-messaging)
         :stream-manager stream-manager))

(deftest messaging
  (with-system (system/system config)
    (let [client (-> {:host (str "localhost:" port)
                      :content-type content-type}
                     (client/client)
                     (client/authenticate {:bottle/username "mike"
                                           :bottle/password "rocket"}))
          bus (:event-bus system)
          last-event (atom nil)
          last-foo (atom nil)
          foo-1 {:bottle/category "foo"
                 :bottle/id "1"
                 :count 4}
          producer (producer system)]

      (s/consume #(reset! last-event %) (bus/subscribe bus :all))
      (s/consume #(reset! last-foo %) (bus/subscribe bus "foo"))

      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= [] body)))

      (producer/produce producer (String. (message/encode "application/transit+json" {:bottle/category "foo" :count 4}) "UTF-8"))

      (Thread/sleep 100)

      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= 1 (count body)))
        (let [event (first body)]
          (is (= #{:bottle/id
                   :bottle/category
                   :bottle/time
                   :bottle/closed?
                   :count}
                 (set (keys event))))
          (is (string? (:bottle/id event)))
          (is (not (:bottle/closed? event)))
          (is (instance? java.util.Date (:bottle/time event)))
          (is (= "foo" (:bottle/category event)))
          (is (= 4 (:count event))))))))

(deftest creating-and-querying-events
  (with-system (system/system config)
    (let [client (-> {:host (str "localhost:" port)
                      :content-type content-type}
                     (client/client)
                     (client/authenticate {:bottle/username "mike"
                                           :bottle/password "rocket"}))
          all-conn (client/connect client)
          foo-conn (client/connect-by-category client "foo")
          bar-conn (client/connect-by-category client "bar")
          baz-conn (client/connect-by-category client "baz")
          bus (:event-bus system)
          last-event (atom nil)
          last-foo (atom nil)
          last-bar (atom nil)
          foo-1 {:bottle/category "foo"
                 :bottle/closed? false
                 :count 4}
          foo-1-created {:bottle/message-type :created :bottle/event foo-1}
          bar-2 {:bottle/category "bar"
                 :bottle/closed? false
                 :name "Bob"}
          bar-2-created {:bottle/message-type :created :bottle/event bar-2}
          foo-3 {:bottle/category "foo"
                 :bottle/closed? false
                 :count 15}
          foo-3-created {:bottle/message-type :created :bottle/event foo-3}
          baz-4 {:bottle/category "baz"
                 :bottle/closed? false
                 :numbers [1 2 3]}
          baz-4-created {:bottle/message-type :created :bottle/event baz-4}
          {:keys [bottle/event-messaging bottle/event-content-type]} config
          producer (producer system)]

      (s/consume #(reset! last-event %) (bus/subscribe bus :all))
      (s/consume #(reset! last-foo %) (bus/subscribe bus "foo"))
      (s/consume #(reset! last-bar %) (bus/subscribe bus "bar"))

      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= {} (map-vals purge body))))

      (unpack-response (client/categories client)
        (is (= 200 status))
        (is (= #{} body)))

      ;; create
      (unpack-response (client/create-event client {:bottle/category "foo" :count 4})
        (is (= 201 status))
        (is (string? (:bottle/id body)))
        (is (instance? java.util.Date (:bottle/time body)))
        (is (= foo-1 (purge body))))

      ;; websockets
      (is (= foo-1-created (purge-message (client/receive! all-conn))))
      (is (= foo-1-created (purge-message (client/receive! foo-conn))))
      (is (= :timeout (purge (client/receive! bar-conn))))

        ;; bus

      (is (= foo-1-created (purge-message @last-event)))
      (is (= foo-1-created (purge-message @last-foo)))
      (is (nil? @last-bar))

      ;; query
      (unpack-response (client/categories client)
        (is (= 200 status))
        (is (= #{"foo"} body)))
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= #{foo-1} (purge-all body))))
      (unpack-response (client/events-by-category client "bar")
        (is (= 200 status))
        (is (= #{} (purge-all body))))
      (unpack-response (client/events-by-category client "foo")
        (is (= 200 status))
        (is (= #{foo-1} (purge-all body))))

      ;; create
      (unpack-response (client/create-event client {:bottle/category "bar" :name "Bob"})
        (is (= 201 status))
        (is (= bar-2 (purge body))))

      ;; websockets
      (is (= bar-2-created (purge-message (client/receive! all-conn))))
      (is (= :timeout (purge-message (client/receive! foo-conn))))
      (is (= bar-2-created (purge-message (client/receive! bar-conn))))

      ;; bus
      (is (= bar-2-created (purge-message @last-event)))
      (is (= foo-1-created (purge-message @last-foo)))
      (is (= bar-2-created (purge-message @last-bar)))

      ;; create
      (unpack-response (client/create-event client {:bottle/category "foo" :count 15})
        (is (= 201 status))
        (is (= foo-3 (purge body))))

      ;; websockets
      (is (= foo-3-created (purge-message (client/receive! all-conn))))
      (is (= foo-3-created (purge-message (client/receive! foo-conn))))
      (is (= :timeout (purge-message (client/receive! bar-conn))))

      ;; bus
      (is (= foo-3-created (purge-message @last-event)))
      (is (= foo-3-created (purge-message @last-foo)))
      (is (= bar-2-created (purge-message @last-bar)))

      ;; query
      (unpack-response (client/categories client)
        (is (= 200 status))
        (is (= #{"foo" "bar"} body)))
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= #{foo-1 bar-2 foo-3} (purge-all body))))
      (unpack-response (client/events-by-category client "bar")
        (is (= 200 status))
        (is (= #{ bar-2} (purge-all body))))
      (unpack-response (client/events-by-category client "foo")
        (is (= 200 status))
        (is (= #{foo-1 foo-3} (purge-all body))))

      ;; create via message
      (producer/produce producer (String. (message/encode "application/transit+json" baz-4) "UTF-8"))

      (Thread/sleep 1000)

      ;; websockets
      (is (= baz-4-created (purge-message (client/receive! all-conn))))
      (is (= :timeout (purge-message (client/receive! foo-conn))))
      (is (= :timeout (purge-message (client/receive! bar-conn))))
      (is (= baz-4-created (purge-message (client/receive! baz-conn))))

      ;; query
      (unpack-response (client/categories client)
        (is (= 200 status))
        (is (= #{"foo" "bar" "baz"} body)))
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= #{foo-1 bar-2 foo-3 baz-4} (purge-all body))))
      (unpack-response (client/events-by-category client "bar")
        (is (= 200 status))
        (is (= #{bar-2} (purge-all body))))
      (unpack-response (client/events-by-category client "foo")
        (is (= 200 status))
        (is (= #{foo-1 foo-3} (purge-all body))))
      (unpack-response (client/events-by-category client "baz")
        (is (= 200 status))
        (is (= #{baz-4} (purge-all body)))))))

(deftest closing-event
  (with-system (system/system config)
    (let [client (-> {:host (str "localhost:" port)
                      :content-type content-type}
                     (client/client)
                     (client/authenticate {:bottle/username "mike"
                                           :bottle/password "rocket"}))
          foo-1 {:bottle/category "foo"
                 :bottle/closed? false
                 :count 4}]
      ;; query
      (unpack-response (client/events client)
        (is (= 200 status))
        (is (= {} (map-vals purge body))))

      ;; create
      (unpack-response (client/create-event client {:bottle/category "foo" :count 4})
        (is (= 201 status))
        (is (string? (:bottle/id body)))
        (is (instance? java.util.Date (:bottle/time body)))
        (is (= foo-1 (purge body)))

        (let [id (:bottle/id body)]
          (unpack-response (client/events client)
            (is (= 200 status))
            (is (= #{foo-1} (purge-all body)))
            (is (= id (:bottle/id (first body)))))

          (unpack-response (client/close-event client id)
            (is (= 200 status))
            (is (= (assoc foo-1 :bottle/closed? true) (purge body))))

          (unpack-response (client/event client id)
            (is (= 200 status))
            (is (= (assoc foo-1 :bottle/closed? true) (purge body))))


          )))))
