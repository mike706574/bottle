(ns bottle.message-test
  (:require [clojure.test :refer [deftest testing is]]
            [cognitect.transit :as transit]
            [bottle.message :as message]))

(deftest encoding
  (testing "application/edn"
    (is (= "{:foo \"bar\"}" (message/encode
                             "application/edn"
                             {:foo "bar"}))))

  (testing "application/transit+json"
    (let [data {:foo "bar"}
          encoded-bytes (message/encode "application/transit+json" data)
          decoded-bytes (transit/read
                         (transit/reader
                          (java.io.ByteArrayInputStream. encoded-bytes) :json))]
      (is (= {:foo "bar"} decoded-bytes))))

  (testing "application/transit+msgpack"
    (let [data {:foo "bar"}
          encoded-bytes (message/encode "application/transit+msgpack" data)
          decoded-bytes (transit/read
                         (transit/reader
                          (java.io.ByteArrayInputStream. encoded-bytes) :msgpack))]
      (is (= {:foo "bar"} decoded-bytes)))))

(deftest decoding
  (testing "application/edn"
    (is (= {:foo "bar"} (message/decode "application/edn" "{:foo \"bar\"}"))))

  (testing "application/transit+json"
    (let [data {:foo "bar"}
          encoded-string (let [out (java.io.ByteArrayOutputStream.)]
                           (transit/write (transit/writer out :json) data)
                           (.toByteArray out))]
      (is (= {:foo "bar"}
             (message/decode
              "application/transit+json"
              encoded-string)))))

  (testing "application/transit+msgpack"
    (let [data {:foo "bar"}
          encoded-string (let [out (java.io.ByteArrayOutputStream.)]
                           (transit/write (transit/writer out :msgpack) data)
                           (.toByteArray out))]
      (is (= {:foo "bar"}
             (message/decode
              "application/transit+msgpack"
              encoded-string))))))
