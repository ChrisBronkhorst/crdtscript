(ns crdt.impl
  [:require [clojure.data.avl :as avl]])

(def default-site (str #uuid"5ec718c1-b647-4993-9537-730ef7b112a2"))

(defn make-log []
  (avl/sorted-map))

(defn last-indexed [m]
  (nth m (dec (count m)) nil))

(defn last-value [m]
  (if-let [[key val] (last-indexed m)]
    val nil))

(defn last-key [m]
  (if-let [[key val] (last-indexed m)]
    key nil))

(defn initial-id [] [0 default-site])

(defn next-id [log site-id]
  (if-let [[counter _] (some-> log last-indexed key)]
    [(inc counter) site-id]
    (initial-id)))


