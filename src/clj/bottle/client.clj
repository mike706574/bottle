(ns bottle.client
  (:require [aleph.http :as http]
            [manifold.stream :as s]
            [bottle.message :as message]))

(def content-type "application/transit+json")

(defn receive!
  ([conn]
   (receive! conn 100))
  ([conn timeout]
   (let [out @(s/try-take! conn :drained timeout :timeout)]
     (if (contains? #{:drained :timeout} out) out (message/decode content-type out)))))

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
  ([ws-url]
   (connect! ws-url nil))
  ([ws-url category]
   (let [url (if category
               (str ws-url "/" (name category))
               ws-url)
         conn @(http/websocket-client url)]
     conn)))

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

;; TODO
(defn get-events
  [http-url]
  (transit-get (str http-url "/api/events")))

(defn get-events-by-category
  [http-url category]
  (parse @(http/get (str http-url "/api/events")
                    {:headers {"Content-Type" content-type
                               "Accept" content-type}
                     :query-params {"category" (name category)}
                     :throw-exceptions false})))

(defn create-event
  [http-url event]
  (transit-post (str http-url "/api/events") event))
