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
  will be both first and last element of the cycle."
  [graph root]
  ;; Since we actually might end up in another cycle, we must backtrack if we've
  ;; found a vertex we've already visited.
  (letfn [(dfs [cycle-list visited to-visit]
            (cond (= to-visit root) (conj cycle-list to-visit)
                  (visited to-visit) nil
                  :else
                  (let [cycle-list (conj cycle-list to-visit)
                        visited (conj visited to-visit)]
                    (first
                     (->> (map #(dfs cycle-list visited %) (get graph to-visit))
                          (remove nil?))))))]
    (first
     (->> (map #(dfs [root] #{} %) (get graph root))
          (remove nil?)))))

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
  "Returns the order to expand aliases in definitions, which ensures no alias is
  unexpanded. Throws an error if there is a cyclic alias."
  [grammar]
  (let [alias-deps (into (zipmap (keys (:aliases grammar))
                                 (repeat #{}))
                         (for [[k v] (:aliases grammar)]
                           [k (find-alias-deps v)]))]
    (toposort-transposed alias-deps)))

(defn- replace-in-expr
  "Replaces all occurences of alias symbols in the given expression with the
  actual alias expression. Will not walk the expanded aliases."
  [grammar expr]
  (case (:type expr)
    (:string :char-set) expr
    :symbol (get-in grammar [:aliases (:value expr)])
    :op (assoc expr
          :args (mapv #(replace-in-expr grammar %) (:args expr)))))

(defn expand-aliases
  "Expands all alias references in both aliases and rules. Will throw an
  exception if there are any circular aliases."
  [grammar]
  (let [expand-order (expand-alias-order grammar)]
    (as-> grammar grammar
          ;; Update aliases
          (reduce (fn [g alias-name]
                    (update-in g [:aliases alias-name]
                               #(replace-in-expr g %)))
                  grammar expand-order)
          ;; Update rules
          (assoc grammar :rules
                 (into {}
                       (for [[r-name r-expr] (:rules grammar)]
                         [r-name (replace-in-expr grammar r-expr)]))))))

;; Argument type checking.

(defn- add-arg-type
  "Attaches the return type of the expression. Will not recompute alias
  expansions; will instead read off the return value by looking up the computed
  value in the grammar."
  [expr grammar]
  (let [result-args (if (and (= :op (:type expr))
                             (not (-> expr meta :alias-expansion)))
                      (mapv #(add-arg-type % grammar) (:args expr)))
        result-type (case (:type expr)
                      :string :string
                      :char-set :char-set
                      :symbol :error
                      ;; ^ Presumably we've done alias expansion, so this
                      ;; shouldn't happen. Perhaps better throw and error out?
                      :op (if (-> expr meta :alias-expansion)
                            (-> grammar ;; Lookup alias result type.
                                (get-in [:aliases (-> expr meta :alias-name)])
                                meta
                                :result)
                            (case (:value expr)
                              (:or :not) :char-set
                              :cat (if (= 1 (count result-args))
                                     (-> (first result-args) meta :result)
                                     :string)
                              (:plus :star :opt) :string)))]
    (cond-> (vary-meta expr assoc :result result-type)
            result-args
            (assoc :args result-args))))

(defn add-arg-types
  "Returns the original grammar, where the metadata key :result has been
  attached to expressions to show their returns types. Argument values are not
  checked for correctness, see `check-arg-type` for such handling."
  [grammar]
  (let [alias-order (expand-alias-order grammar)
        grammar (reduce (fn [g a-name]
                          (update-in g [:aliases a-name] add-arg-type g))
                        grammar
                        alias-order)]
    (assoc grammar
      :rules (reduce
              (fn [m r-name]
                (update-in m [r-name] add-arg-type grammar))
              (:rules grammar)
              (keys (:rules grammar))))))

