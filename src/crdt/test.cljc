(ns crdt.test
  [:require [crdt.datascript :as cdt]
            [clojure.data :as data]])


(defn random-data []
  (for [i (range 1000)]
    {:user/id i
     :data i}))

(defn random2 []
  [{:user/id 1 :there/there "myboy"}])

(defn random3 [] [{:user/id 1 :my/son "myson"}])

(comment

  (let [crdt1 (-> (cdt/make-crdt :site-id "site1" :schema {:user/id {:db/unique :db.unique/identity}})
                  (cdt/transact (random-data)))
        crdt2 (-> (cdt/clone crdt1 "site2")
                  (cdt/transact (random2)))
        crdt3 (-> (cdt/clone crdt1 "site3")
                  (cdt/transact (random3)))
        db (-> crdt1
               (cdt/squash (:log crdt2))
               (cdt/squash (:log crdt3)))]
    (cdt/q
      '[:find ?e ?a ?v
        :in $ $2
        :where  [$ ?e ?a ?v]
                (not [$2 ?e ?a ?v])]
      db crdt1))

  (let [crdt1 (-> (cdt/make-crdt :site-id "site1" :schema {:user/id {:db/unique :db.unique/identity}})
                  (cdt/transact (random-data)))
        crdt2 (-> (cdt/clone crdt1 "site2")
                  (cdt/transact (random2)))
        crdt3 (-> (cdt/clone crdt1 "site3")
                  (cdt/transact (random3)))
        db (time (-> crdt1
                     (cdt/squash (:log crdt2))
                     (cdt/squash (:log crdt3))))]
    db)

  1)