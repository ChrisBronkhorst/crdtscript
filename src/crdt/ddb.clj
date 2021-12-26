(ns crdt.ddb
  (:require [taoensso.faraday :as far]))

(def client-opts
  {;;; For DynamoDB Local just use some random strings here, otherwise include your
   ;;; production IAM keys:

   ;;; You may optionally override the default endpoint if you'd like to use DynamoDB
   ;;; Local or a different AWS Region (Ref. http://goo.gl/YmV80o), etc.:
   ;; :endpoint "http://localhost:8000"                   ; For DynamoDB Local
   ;; :endpoint "http://dynamodb.eu-west-1.amazonaws.com" ; For EU West 1 AWS region

   ;;; You may optionally provide your own (pre-configured) instance of the Amazon
   ;;; DynamoDB client for Faraday functions to use.
   ;; :client (AmazonDynamoDBClientBuilder/defaultClient)
   :access-key "<AWS_DYNAMODB_ACCESS_KEY>"
   :secret-key "<AWS_DYNAMODB_SECRET_KEY>"
   :endpoint "http://localhost:8000"})

(def crdt-table :bimwatcher-crdts)

(defn create-crdt-table []
  (far/create-table client-opts crdt-table
                    [:db :s]
                    {:range-keydef [:id :s]
                     :billing-mode :pay-per-request
                     :lsindexes [{:name "counter-index"
                                  :range-keydef [:cn :n]}
                                 {:name "site-index"
                                  :range-keydef [:s :s]}]
                     :block? true}))

(defn to-ddb [crdt-name crdt-op]
  (let [{:keys [id ]} crdt-op
        [counter site-id] id]
    {:db crdt-name
     :id (str counter "-" site-id)
     :s (str site-id)
     :cn counter
     :tx (far/freeze crdt-op)}))

(defn to-crdt [crdt-op] (:tx crdt-op))

(defn save-tx! [crdt-name crdt-op]
  (far/put-item client-opts crdt-table
      (to-ddb crdt-name crdt-op)))

(defn get-crdt!
  ([crdt-name]
   (far/query
     client-opts
     crdt-table
     {:db [:eq crdt-name]}))
  ([crdt-name query]
   (far/query
     client-opts crdt-table
     (merge {:db [:eq crdt-name]} query)
     {:index "counter-index"}))
  ([crdt-name query opts]
   (far/query
     client-opts crdt-table
     (merge {:db [:eq crdt-name]} query)
     (merge {:index "counter-index"} opts))))

(comment
  (far/list-tables client-opts)
  (create-crdt-table)
  (far/delete-table client-opts crdt-table)

  (time (save-tx! "tx" (create-crdt-tx [0 default-site] [{:id 1 :data 5}])))
  (save-tx! "tx" (create-crdt-tx [3 default-site] [{:id 1 :data 5}]))



  (get-crdt! "tx" {:cn [:between [1 100]]})
  (get-crdt! "tx")

  (time (far/query
          client-opts
          crdt-table
          {:db [:eq "tx"]
           :cn [:between [1 100]]}
          {:index "counter-index"}))

  (time (far/query
          client-opts
          crdt-table
          {:db [:eq "tx"]
           :s [:eq default-site]}
          {:index "site-index"})))
