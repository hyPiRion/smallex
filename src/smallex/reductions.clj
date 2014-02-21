(ns smallex.reductions
  (:require [clojure.set :as set]
            [clojure.walk :as walk]
            [smallex.utils :as utils :refer [map-vals]]))

(def ^:private full-char-set
  "A full char-set of all possible bytes"
  (set (map char (range 0x100))))

(defn- op-of
  [op-type]
  (fn [expr]
    (and (= :op (:type expr))
         (= op-type (:value expr)))))

(def ^:private cat-op? (op-of :cat))
(def ^:private or-op? (op-of :or))
(def ^:private not-op? (op-of :not))
(def ^:private star-op? (op-of :star))
(def ^:private plus-op? (op-of :plus))
(def ^:private opt-op? (op-of :opt))

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
          (update-in grammar [:rules] map-vals #(replace-in-expr grammar %)))))

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
                              :not :char-set
                              :or (if (every? #(-> % meta :result (= :char-set))
                                              (:args expr))
                                    :char-set
                                    :string)
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

(defn- setify-char-set
  "Converts a char-set to an actual set."
  [char-set]
  (update-in char-set [:value] set))

(defn- invert-not
  "Inverses the not operation to a char-set."
  [not-op]
  {:type :char-set
   :value (set/difference full-char-set
                          (get-in not-op [:args 0 :value]))})

(defn- flatten-cat
  "Flattens a `cat`, whenever possible."
  [cat-op]
  (assoc cat-op
    :args (->> (:args cat-op)
               (mapcat (fn [arg] (if (cat-op? arg)
                                  (:args arg)
                                  [arg])))
               vec)))

(defn- reduce-cat
  "Reduces a `cat`, whenever possible."
  [cat-op]
  (cond (every? #(= :string (:type %)) (:args cat-op))
        {:type :string, :value (apply str (map :value (:args cat-op)))}
        (= 1 (count (:args cat-op)))
        (first (:args cat-op))
        :else
        cat-op))

(defn- flatten-or
  "Flattens an `or`, whenever possible."
  [or-op]
  (assoc or-op
    :args (->> (:args or-op)
               (mapcat (fn [arg] (if (or-op? arg)
                                  (:args arg)
                                  [arg])))
               vec)))

(defn- reduce-or
  "Reduces an `or` whenever possible."
  [or-op]
  (cond (every? #(= :char-set (:type %)) (:args or-op))
        {:type :char-set,
         :value (apply set/union (map :value (:args or-op)))}
        (= 1 (count (:args or-op)))
        (first (:args or-op))
        :else
        or-op))

(defn- expand-opt
  "Expands (opt x) to (or \"\" x)."
  [opt-op]
  {:type :op, :value :or
   :args [{:type :string, :value ""}
          (get-in opt-op [:args 0])]})

(defn- expand-plus
  "Expands (plus x) to (cat x (star x))."
  [plus-op]
  {:type :op, :value :cat
   :args [(get-in plus-op [:args 0])
          {:type :op, :value :star,
           :args (:args plus-op)}]})

(defn- reduce-expression
  "Reduces an expression."
  [expr]
  (case (:type expr)
    :op (let [expr (assoc expr
                     :args (mapv reduce-expression (:args expr)))]
          (case (:value expr)
            :cat (-> expr flatten-cat reduce-cat)
            :or (-> expr flatten-or reduce-or)
            :opt (expand-opt expr)
            :plus (expand-plus expr)
            :star expr))
    :char-set (setify-char-set expr)
    :string expr))

(defn reduce-rules
  "Reduces all rules within the grammar."
  [grammar]
  ;; TODO: memoize alias reductions perhaps?
  (update-in grammar [:rules] map-vals reduce-expression))

