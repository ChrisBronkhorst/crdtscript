(ns crdt.ops
  [:require [clojure.spec.alpha :as s]])

(s/def ::crdt-name (s/or :uuid uuid?))

(s/def ::id (s/cat :counter int? :site-id string?))

(s/def ::type #{::datascript-tx ::snapshot})
(s/def ::tx-data vector?)
(s/def ::tx (s/keys :req [::id ::type]
                    :opt [::tx-data]))

(defn datascript-tx [[counter site-id] tx-data]
  {::id [counter site-id]
   ::type ::datascript-tx
   ::tx-data tx-data})

(comment
  (let [op (datascript-tx [0 default-site] [{:db 1 :script "hi"}])]
    (s/explain ::tx op))
  (let [op (datascript-tx [0 default-site] [])]
    (s/explain ::tx op)))