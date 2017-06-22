(defproject org.clojars.mike706574/bottle "0.0.1-SNAPSHOT"
  :description "Describe me!"
  :url "https://github.com/mike706574/bottle-webapp"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/spec.alpha "0.1.123"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.3.443"]
                 [com.stuartsierra/component "0.3.2"]

                 ;; Utility
                 [com.cognitect/transit-clj "0.8.300"]
                 [manifold "0.1.6"]
                 [byte-streams "0.2.3"]
                 [environ "1.1.0"]
                 [clj-time "0.13.0"]

                 ;; Messaging
                 [com.rabbitmq/amqp-client "4.1.1"]
                 [org.apache.activemq/activemq-core "5.7.0"]

                 ;; Logging
                 [com.taoensso/timbre "4.10.0"]

                 ;; Database
                 [org.clojure/java.jdbc "0.6.1"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.2"]
                 [postgresql "9.3-1102.jdbc41"]
                 [mysql/mysql-connector-java "6.0.6"]

                 ;; Web
                 [aleph "0.4.3"]
                 [ring/ring-anti-forgery "1.1.0"]
                 [ring-cors "0.1.10"]
                 [ring/ring-defaults "0.3.0"]
                 [ring-middleware-format "0.7.2"]
                 [compojure "1.6.0"]
                 [selmer "1.10.7"]

                 ;; Security
                 [buddy/buddy-hashers "1.2.0"]
                 [buddy/buddy-sign "1.5.0"]]
  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["test/clj"]
  :plugins [[cider/cider-nrepl "0.15.0-SNAPSHOT"]
            [org.clojure/tools.nrepl "0.2.12"]
            [lein-cloverage "1.0.9"]]
  :profiles {:dev {:source-paths ["dev"]
                   :target-path "target/dev"
                   :dependencies [[org.clojure/test.check "0.9.0"]
                                  [org.clojure/tools.namespace "0.2.11"]]}
             :production {:aot :all
                          :main bottle.server.main
                          :uberjar-name "bottle-webapp.jar"}})
