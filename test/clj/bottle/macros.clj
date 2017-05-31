(ns bottle.macros
  (:require [com.stuartsierra.component :as component]))

(defmacro with-component
  [constructor config & body]
  `(let [~'component (component/start (~constructor ~config))]
     (try
       ~@body
       (finally (component/stop ~'component)))))
