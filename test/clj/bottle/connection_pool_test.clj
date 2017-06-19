(ns bottle.connection-pool-test
  (:require [bottle.connection-pool :as connection-pool]
            [bottle.macros :refer [with-component]]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing]]))

(deftest connection-pool
  (testing "postgres connection pool"
    (with-component connection-pool/connection-pool {:bottle.database/driver-class "org.postgresql.Driver"
                                                     :bottle.database/subprotocol "postgresql"
                                                     :bottle.database/subname "//qdspgdev.qg.com/jsr"
                                                     :bottle.database/username "jsr"
                                                     :bottle.database/password "qdsjsr"}
      (is (= [{:?column? 1}] (jdbc/query component ["select 1"])))
      (is (= [{:?column? 2}] (jdbc/query component ["select 2"]))))))
