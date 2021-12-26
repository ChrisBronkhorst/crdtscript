(ns crdt.util
  #?(:clj [:import java.util.UUID]))

(defn make-uuid []
  #?(:clj (UUID/randomUUID)
     :cljs (random-uuid)))
