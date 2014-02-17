(ns smallex.reductions
  (:require [clojure.set :as set]
            [clojure.walk :as walk]))

(defn- find-alias-deps
  "Returns a set of alias deps, given an expression."
  [expr]
  (case (:type expr)
    (:string :char-set) #{}
    :symbol #{(:value expr)}
    :op (apply set/union (map find-alias-deps (:args expr)))))

(def ^:private set-conj
  (fnil conj #{}))

(defn- transpose-graph
  "Given a mapping from a vertex to a set of vertices, reverse the edge
  direction."
  [graph]
  (reduce-kv
   (fn [transposed vertex pointers]
     (reduce
      (fn [acc ptr]
        (update-in acc [ptr] set-conj vertex))
      transposed
      pointers))
   {}
   graph))

(defn check-cyclic-aliases
  "Given a grammar, returns an error if there is a cyclic alias. Returns nil if
  the aliases aren't cyclic."
  [grammar]
  (let [alias-deps (into {} (for [[k v] (:aliases grammar)]
                              [k (find-alias-deps v)]))]
    ;; TODO: Topological sort here.
    ))
