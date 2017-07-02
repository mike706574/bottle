(ns bottle.database.connection-pool-test
  (:require [bottle.database.connection-pool :as connection-pool]
            [bottle.macros :refer [with-component]]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing]]))

(defn test-pool
  [config]
  (with-component connection-pool/connection-pool config
    (is (= [{:?column? 1}] (jdbc/query component ["select 1"])))
    (is (= [{:?column? 2}] (jdbc/query component ["select 2"])))))


#_(deftest mysql-connection-pool
    (test-pool #:bottle.database{:driver-class "com.mysql.jdbc.Driver"
                                 :subprotocol "mysql"
                                 :host "localhost"
                                 :port 3306
                                 :database "mysql"
                                 :username "mysql"
                                 :password "mysql"}))

(deftest postgres-connection-pool
  (test-pool #:bottle.database{:driver-class "org.postgresql.Driver"
                               :subprotocol "postgresql"
                               :host "localhost"
                               :port 5432
                               :database "postgres"
                               :username "postgres"
                               :password "postgres"}))
