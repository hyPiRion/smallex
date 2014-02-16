(ns smallex.reductions
  (:require [clojure.walk :as walk]))

(defn- find-alias-deps
  "Returns a set of alias deps, given an expression."
  [expr]
  (case (:type expr)
    (:string :char-set) #{}
    :symbol #{(:value expr)}
    :op (set (mapcat find-alias-deps (:args expr)))))

(defn check-cyclic-aliases
  "Given a grammar, returns an error if there is a cyclic alias. Returns nil if
  the aliases aren't cyclic."
  [grammar]
  (let [alias-deps (into {} (for [[k v] (:aliases grammar)]
                              [k (find-alias-deps v)]))]
    ;; TODO: Topological sort here.
    ))
