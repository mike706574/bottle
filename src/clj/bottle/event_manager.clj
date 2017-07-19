(ns bottle.event-manager
  (:require [clojure.spec.alpha :as s]
            [bottle.specs]
            [bottle.util :as util]
            [taoensso.timbre :as log]))

(defprotocol EventManager
  "Manages events."
  (events [this] [this options] "Retrieves events")
  (page [this page-size page-number] "TODO")
  (categories [this] "Retrieves all categories.")
  (close! [this id] "Closes an event.")
  (add! [this data] "Add an event."))

(def date-comparator #(compare (:bottle/time %1) (:bottle/time %2)))

(defn prop-xform
  [option-key prop-key]
  (fn prop [options]
    (let [option (get options option-key)]
      (when-not (nil? option)
        (filter #(= (get % prop-key) option))))))

(defn category-xform
  [{category :category}]
  (when category
    (filter #(= (:bottle/category %) (keyword category)))))

(defn page-xform
  [{page-number :page-number page-size :page-size :as options}]
  (cond
    (and page-number page-size) (comp (drop (* page-size (dec page-number)))
                                      (take page-size))
    (or page-number page-size) (throw (ex-info "Both page-number and page-size are required for pagination." options))))

(def factories [page-xform
                category-xform
                (prop-xform :closed? :bottle/closed?)])

(defrecord RefEventManager [counter events]
  EventManager
  (events [this]
    @events)

  (events [this options]
    (let [xforms (->> factories
                     (map #(% options))
                     (filter identity))]
      (into [] (apply comp xforms) (vals @events))))

  (categories [this]
    (set (map :bottle/category (vals @events))))

  (close! [this id]
    (dosync
     (when (contains? @events id)
       (alter events assoc-in [id :bottle/closed?] true)
       (get @events id))))

  (add! [this template]
    (dosync
     (let [id (str (alter counter inc))
           event (assoc template
                        :bottle/id id
                        :bottle/time (java.util.Date.)
                        :bottle/closed? false)]
       (log/debug (str "Storing event " event "."))
       (alter events assoc id event)
       event))))

(defn event-manager [config]
  (map->RefEventManager
   {:counter (ref 0)
    :events (ref {})}))

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
