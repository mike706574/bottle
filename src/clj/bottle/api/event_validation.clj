(ns bottle.api.event-validation
  (:require [bottle.util :as util]
            [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]))

(defn validate-event-template
  [event-template]
  (when-let [validation-failure (s/explain-data :bottle/event-template event-template)]
    (log/error (str "Invalid event template:\n"
                    (util/pretty event-template)
                    "Validation failure:"
                    (util/pretty validation-failure)))
    validation-failure))
