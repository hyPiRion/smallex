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
  "Given a mapping from a vertex to a set of vertices, reverses the edge
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

(defn- track-cycle
  "Returns a vector with the elements of _some_ cycle which contains root. Root
  will be both first and last element."
  [graph root]
  ;; TODO
  )

(defn- toposort-transposed
  "Returns a topological sort of transposed graph, and throws an exception if
  the graph is cyclic."
  [graph]
  (let [inv (transpose-graph graph)]
    ;; Can, strictly speaking, do an (vec (rseq original)) instead
    (letfn [(dfs [[stack visited] to-visit]
              (case (get visited to-visit :not-visited)
                :walking
                (throw (ex-info "Found cycle."
                                {:type :cycle
                                 :cycle (track-cycle graph to-visit)}))
                :completed [stack visited]
                :not-visited
                (let [visited (assoc visited to-visit :walking)
                      [stack visited]
                      (reduce dfs [stack visited] (get inv to-visit))]
                  [(conj stack to-visit)
                   (assoc visited to-visit :completed)])))]
      (first
       (reduce dfs [[] {}] (keys graph))))))

(defn expand-alias-order
  "Given a grammar, returns the order to expand aliases in definitions, which
  ensures no alias is unexpanded. Throws an error if there is a cyclic alias."
  [grammar]
  (let [alias-deps (into (zipmap (keys (:aliases grammar))
                                 (repeat #{}))
                         (for [[k v] (:aliases grammar)]
                           [k (find-alias-deps v)]))]
    (toposort-transposed alias-deps)))
