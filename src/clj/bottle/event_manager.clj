(ns bottle.event-manager
  (:require [clojure.spec.alpha :as s]
            [bottle.specs]
            [bottle.util :as util]
            [taoensso.timbre :as log]))

(defprotocol EventManager
  "Manages events."
  (event [this id] "Retrieves an event.")
  (events [this] [this options] "Retrieves events.")
  (page [this page-size page-number] "TODO")
  (categories [this] "Retrieves all categories.")
  (close! [this id] "Closes an event.")
  (add! [this data] "Add an event."))

(defmulti event-manager :bottle/event-manager-type)

(defmethod event-manager :default
  [{event-manager-type :bottle/event-manager-type :as config}]
  (throw (ex-info (str "Invalid event manager type "
                       (name event-manager-type))
                  (or config {}))))

(s/def :bottle/event-manager (partial satisfies? EventManager))

(defn ^:private submap?
  [sub sup]
  (= sub (select-keys sup (keys sub))))

(s/fdef add!
  :args (s/cat :event-manager :bottle/event-manager
               :event-template :bottle/event-template)
  :ret :bottle/event
  :fn #(submap? (-> % :args :event-template) (:ret %)))

(s/fdef events
  :args (s/cat :event-manager :bottle/event-manager)
  :ret (s/map-of string? :bottle/event)
  :fn #(submap? (-> % :args :event-template) (:ret %)))
