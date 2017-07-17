(ns bottle.event-manager-test
  (:require [clojure.test :refer [deftest is]]
            [bottle.event-manager :as event-manager]))


(comment
  (let [manager (event-manager/event-manager {})]
    (event-manager/add! manager {:bottle/category :foo :number 1})
    (event-manager/close! manager "1")
    (event-manager/add! manager {:bottle/category :bar :number 1})

    (event-manager/events manager {:category "foo"})
    )

  
  


  (def events (event-manager/events manager))
  (is (= 50 (count events)))


  (event-manager/page* events 10 -1)

  )

