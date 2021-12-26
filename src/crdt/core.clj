(ns crdt.core
  [:require [editscript.core :as e]
            [clojure.data.avl :as avl]
            [datascript.core :as ds]])

(defn last-indexed [m]
  (nth m (dec (count m)) nil))

(defn next-id [log site-id]
  (if-let [[counter _] (some-> log last-indexed key)]
    [(inc counter) site-id]
    [0 nil]))

(defn materialize [log]
  (reduce e/patch nil (vals log)))

(defn transact [log site-id f]
  (let [old (materialize log)
        new (f old)
        diff (e/diff old new {:str-diff? true :algo :quick})]
    (assoc log (next-id log site-id) diff)))

(defn transact2 [log site-id tx]
  (assoc log (next-id log site-id) tx))

(defn materialize2 [log schema]
  (reduce ds/db-with (ds/empty-db schema) (vals log)))

(comment
  (let [log  (-> (avl/sorted-map)
                 (transact :site1 (fn [x] {:a {:element 1 :text "hi"}
                                           :b {:element 2 :text "there"
                                               :vec [1 2 3]}})))
        log2 (-> log
                 (transact :site2 (fn [x] (assoc-in x [:a :text] "myman")))
                 (transact :site2 (fn [x] (assoc-in x [:b :text] "myman"))))
        log3 (-> log
                 (transact :site3 (fn [x] (assoc-in x [:a :text] "myman2")))
                 (transact :site3 (fn [x] (update-in x [:a :text] subs 3)))
                 (transact :site3 (fn [x] (update-in x [:b :vec] subvec 1))))]
    (materialize (merge log log2 log3)))

  (let [log  (-> (avl/sorted-map)
                 (transact2 :site1 [{:id "one"
                                     :hello "world"}]))
        log2 (-> log
                 (transact2 :site2 [{:id "one" :hello "world 2"}])
                 (transact2 :site2 [[:db/retractEntity [:id "one"]]]))
        log3 (-> log
                 (transact2 :site3 [{:id "three"
                                     :hello "world"}]))
        log4 (-> log3
                 (transact2 :site4 [{:id "one" :hello "site4"}]))]
    (ds/q '[:find ?id ?message
            :where [?e :hello ?message]
                   [?e :id ?id]]
       (materialize2 (merge log log2 log3 log4) {:id {:db/unique :db.unique/identity}}))))

