(ns bottle.client
  (:require [aleph.http :as http]
            [manifold.stream :as s]
            [boomerang.message :as message]
            [bottle.users :as users]))

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
  [response]
  (let [response-content-type (get-in response [:headers "content-type"])]
    (if (and (contains? response :body) (= response-content-type content-type))
      (update response :body (comp (partial message/decode content-type)))
      response)))



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

(defn add-user!
  [system username password]
  (users/add! (:user-manager system) {:bottle/username username
                                             :bottle/password password}))

(defn http-url [host] (str "http://" host))
(defn ws-url [host] (str "ws://" host))

(defn connect!
  ([host token]
   (connect! host token nil))
  ([host token category]
   (let [endpoint-url (str (ws-url host) "/api/websocket")
         url (if category
               (str endpoint-url "/" (name category))
               endpoint-url)
         url (str url "?token=" token)
         conn @(http/websocket-client url)]
     conn)))

(defprotocol Client
  (authenticate [this credentials])
  (connect [this])
  (connect-by-category [this cateogry])
  (categories [this])
  (events [this])
  (events-by-category [this category])
  (create-event [this event]))

(defrecord ServiceClient [host content-type token]
  Client
  (authenticate [this credentials]
    (let [response @(http/post (str (http-url host) "/api/tokens")
                               {:headers {"Content-Type" content-type
                                          "Accept" "text/plain"}
                                :body (message/encode content-type credentials)
                                :throw-exceptions false})]
      (when (= (:status response) 201)
        (assoc this :token (-> response :body slurp)))))

  (connect [this]
    (connect! host token nil))

  (connect-by-category [this category]
    (connect! host token category))

  (categories [this]
    (parse @(http/get (str (http-url host) "/api/categories")
                      {:headers {"Content-Type" content-type
                                 "Accept" content-type
                                 "Authorization" (str "Token " token)}
                       :throw-exceptions false})))

  (events [this]
    (parse @(http/get (str (http-url host) "/api/events")
                      {:headers {"Content-Type" content-type
                                 "Accept" content-type
                                 "Authorization" (str "Token " token)}
                       :throw-exceptions false})))

  (events-by-category [this category]
    (parse @(http/get (str (http-url host) "/api/events")
                      {:headers {"Content-Type" content-type
                                 "Accept" content-type
                                 "Authorization" (str "Token " token)}
                       :query-params {"category" (name category)}
                       :throw-exceptions false})))
  (create-event [this event]
    (parse @(http/post (str (http-url host) "/api/events")
                       {:headers {"Content-Type" content-type
                                  "Accept" content-type
                                  "Authorization" (str "Token " token)}
                        :body (message/encode content-type event)
                        :throw-exceptions false}))))

(defn client
  [{:keys [host content-type]}]
  (map->ServiceClient {:host host
                       :content-type content-type}))
