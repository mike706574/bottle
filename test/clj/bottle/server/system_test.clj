(ns bottle.server.system-test
  (:require [aleph.http :as http]
            [com.stuartsierra.component :as component]
            [clojure.test :refer [deftest testing is]]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [bottle.server.system :as system]
            [bottle.server.message :refer [encode decode]]
            [taoensso.timbre :as log]))

(def config {:id "test" :port 10000})

(defmacro with-system
  [& body]
  (let [port (:port config)
        ws-url (str "ws://localhost:" port "/api/websocket")]
    `(let [~'system (component/start-system (system/system config))
           ~'ws-url ~ws-url
           ~'http-url #(str "http://localhost:" ~port %)]
       (try
         ~@body
         (finally (component/stop-system ~'system))))))

(defn receive!
  [conn]
  (let [out @(s/try-take! conn :drained 2000 :timeout)]
    (if (contains? #{:drained :timeout} out) out (decode out))))

(defn flush!
  [conn]
  (loop [out :continue]
    (when (not= out :timeout)
      (recur @(s/try-take! conn :drained 10 :timeout)))))

(defn send!
  [conn message]
  (s/put! conn (encode message)))

(defn parse
  [request]
  (if (contains? request :body)
    (update request :body (comp decode slurp))
    request))

;; TODO: Handle errors
(defn connect!
  [ws-url id]
  (let [conn @(http/websocket-client ws-url)]
    (send! conn id)
    (is (not= :timeout
              (receive! conn)))
    conn))

(deftest connecting
  (with-system
    (let [mike-conn (connect! ws-url "mike")]
      (println "Connected!"))))

(defn transit-get
  [url]
  (parse @(http/get url
                    {:headers {"Content-Type" "application/transit+json"
                               "Accept" "application/transit+json"}
                     :throw-exceptions false})))

(defn transit-post
  [url body]
  (parse @(http/post url
                     {:headers {"Content-Type" "application/transit+json"
                                "Accept" "application/transit+json"}
                      :body (encode body)
                      :throw-exceptions false})))

(comment
  (transit-get "http://localhost:8001/api/events")
  (transit-post "http://localhost:8001/api/events" {:bottle/event-type :foo})


  )
