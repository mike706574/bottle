(ns bottle.connection-pool-test
  (:require [bottle.connection-pool :as connection-pool]
            [bottle.macros :refer [with-component]]
            [clojure.test :refer [deftest is testing]]))

(deftest whatever
  (testing "whatever"
    (with-component connection-pool/connection-pool {:subprotocol "postgresql"
                                                     :subname "bottle-test"
                                                     :username "posgres"
                                                     :password "postgres"}




      )


    )

)
