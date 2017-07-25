(ns bottle.specs
  (:require [clojure.spec.alpha :as s]))

(s/def :bottle/id string?)
(s/def :bottle/category string?)
(s/def :bottle/event-template (s/and (s/keys :req [:bottle/category])
                                     #(not (contains? % :bottle/id))))
(s/def :bottle/event (s/keys :req [:bottle/id
                                   :bottle/category
                                   :bottle/time]))
