(ns crdt.datascript
  [:require [datascript.core :as ds]
            [clojure.data.avl :as avl]
            [crdt.ops :as ops]
            [crdt.impl :refer [next-id make-log initial-id last-indexed last-value last-key]]
            [crdt.util :refer [make-uuid]]
            [clojure.repl :refer :all]])

(defprotocol CRDT
  (transact [this transaction])
  (materialize [this] [this key])
  (db [this])
  (get-log [this])
  (squash [this log])
  (-cache [this]))

(declare ->DatascriptCRDT)

(defn build-cache [{:keys [log schema cache]}]
  (let [history (partition 2 1 (conj log [nil nil]))]
    (reduce (fn [acc [prev next]]
              (let [[old-id _] prev
                    [next-id next-tx] next
                    db (or (get acc old-id) (ds/empty-db schema))]
                (assoc acc next-id (ds/db-with db (::ops/tx-data next-tx)))))
            cache
            history)))

(defrecord DatascriptCRDT [log site-id schema cache]
  CRDT
  (db [this] (or  (last-value cache) (ds/empty-db schema)))

  (transact [this tx-data]
    (let [id (next-id log site-id)
          op (ops/datascript-tx id tx-data)
          new-db (ds/db-with (db this) tx-data)]
      (->DatascriptCRDT (assoc log id op) site-id schema (assoc cache id new-db))))

  (get-log [this] log)

  (materialize [this]
      (last-value cache))

  (materialize [this key] (cache key))

  ; this is the naive implementation
  ; you don't need to redo all of the work each time
  (squash [this new-log]
    (let [log (merge log new-log)]
      (-cache (->DatascriptCRDT log site-id schema cache))))

  (-cache [this] (->DatascriptCRDT log site-id schema (build-cache this))))

(defn crdt? [x]
  (= (type x) DatascriptCRDT))

(defn q
  ([query & inputs] (apply ds/q query (map (fn [x] (if (crdt? x) (db x) x)) inputs))))

(defmethod print-method DatascriptCRDT [v ^java.io.Writer w]
  (doto w (.write (str (:log v)))))

(defn make-crdt
  [ & {:keys [log site-id schema]}]
  (-cache (->DatascriptCRDT (or log (make-log))
                            (or site-id (str (make-uuid)))
                            (or schema {})
                            (avl/sorted-map))))

(defn clone [crdt site-id]
  (->DatascriptCRDT (:log crdt) site-id (:schema crdt) (:cache crdt)))








