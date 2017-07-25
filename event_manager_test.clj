(ns bottle.event-manager-test
  (:require [clojure.test :refer [deftest is]]
            [bottle.event-manager :as event-manager]
            [bottle.event-manager.ref]
            [bottle.sanitation :refer [purge purge-all]]))

(defn aliases
  [manager]
  {:all-events #(purge-all (event-manager/events manager))
   :events #(purge-all (event-manager/events manager %))
   :add! #(event-manager/add! manager %)
   :close! #(purge (event-manager/close! manager %))})

(def foo-1 {:bottle/category "foo", :number 1, :bottle/closed? false})
(def bar-2 {:bottle/category "bar", :number 2, :bottle/closed? false})

(def ref-manager #(event-manager/event-manager {:bottle/event-manager-type :ref}))

(deftest empty
  (let [{:keys [all-events events add! close!]} (aliases (ref-manager))]
    (is (= #{} (all-events)))
    (is (= #{} (events {})))
    (is (= #{} (events {:bottle/category "foo"})))
    (is (= #{} (events {:bottle/category "bar"})))
    (is (= #{} (events {:bottle/category "baz"})))
    (is (= #{} (events {:bottle/closed? true})))
    (is (= #{} (events {:bottle/closed? false})))))

(deftest just-foo-1
  (let [{:keys [all-events events add! close!]} (aliases (ref-manager))]
    (add! foo-1)
    (is (= #{foo-1} (all-events)))
    (is (= #{foo-1} (events {})))
    (is (= #{foo-1} (events {:bottle/category "foo"})))
    (is (= #{} (events {:bottle/category "bar"})))
    (is (= #{} (events {:bottle/category "baz"})))
    (is (= #{foo-1} (events {:bottle/closed? false})))
    (is (= #{} (events {:bottle/closed? true})))))

(deftest foo-1-and-bar-2
  (let [{:keys [all-events events add! close!]} (aliases (ref-manager))]
    (add! foo-1)
    (add! bar-2)
    (is (= #{foo-1 bar-2} (all-events)))
    (is (= #{foo-1 bar-2} (events {})))
    (is (= #{foo-1} (events {:bottle/category "foo"})))
    (is (= #{bar-2} (events {:bottle/category "bar"})))
    (is (= #{} (events {:bottle/category "baz"})))
    (is (= #{foo-1 bar-2} (events {:bottle/closed? false})))
    (is (= #{} (events {:bottle/closed? true})))))

(deftest closing
  (let [{:keys [all-events events add! close!]} (aliases (ref-manager))
        id (:bottle/id (add! foo-1))
        closed-foo-1 (assoc foo-1 :bottle/closed? true)]
    (is (= #{foo-1} (events {:bottle/closed? false})))
    (is (= #{} (events {:bottle/closed? true})))
    (is (= #{foo-1} (events {:bottle/category "foo"
                             :bottle/closed? false})))
    (is (= #{} (events {:bottle/category "bar"
                        :bottle/closed? false})))

    (close! id)

    (is (= #{} (events {:bottle/closed? false})))
    (is (= #{closed-foo-1} (events {:bottle/closed? true})))

    (is (= #{closed-foo-1} (events {:bottle/category "foo"
                                    :bottle/closed? true})))
    (is (= #{} (events {:bottle/category "bar"
                        :bottle/closed? true})))))
