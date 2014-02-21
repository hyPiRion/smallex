(ns smallex.grammar-checks)

(def ^:private single-arg-op?
  "Returns true if the op is a single-argument op, false otherwise."
  #{:star :plus :opt :not})

(defn- op-arity-exn
  "Returns a list with either zero or one ExceptionInfos, depending on whether
  the expression uses wrong arity or not."
  [expr-name op]
  (cond (and (single-arg-op? (:value op))
             (not= 1 (count (:args op))))
        (list (ex-info "Wrong number of arguments given."
                       {:type :op-arity, :expr op,
                        :expected "one", :in expr-name}))
        ;; otherwise is a vararg, so is only error if it has zero args
        (zero? (count (:args op)))
        (list (ex-info "Wrong number of arguments given."
                       {:type :op-arity, :expr op,
                        :expected "at least one", :in expr-name}))
        :else ()))

(defn- check-expr-arity
  "Recursively checks that an expression uses correct arity, and returns a
  lazy seq of the erroneous calls as ExceptionInfos."
  [expr-name expr]
  (cond (not= :op (:type expr)) ()
        (-> expr meta :alias-expansion) () ;; Should be handled in resp. aliases
        :else
        (let [some-exn (op-arity-exn expr-name expr)]
          (->> (:args expr)
               (map #(check-expr-arity expr-name %))
               (apply concat)
               (concat some-exn)))))

(defn check-arity
  "Checks that all operations have correct arity. Returns a sequence of
  erroneous calls as ExceptionInfos."
  [grammar]
  (concat
   (mapcat (fn [[a-name a-expr]]
             (->> (vary-meta a-expr dissoc
                             :alias-expansion :alias-name)
                  (check-expr-arity a-name)))
           (:aliases grammar))
   (mapcat (fn [[r-name r-expr]]
             (check-expr-arity r-name r-expr))
           (:rules grammar))))

(defn- check-expr-arg-type
  "Recursively checks that an expression has correct input arg types, and
  returns a lazy seq of the erroneous calls as ExceptionInfos."
  [expr]
  (if (and (= :op (:type expr))
           (not (-> expr meta :alias-expansion)))
    (concat
     (case (:value expr)
       (:cat :opt :plus :star :or) nil
       :not (if (-> expr :args first meta :value (not= :char-set))
              (list
               (ex-info "`not` requires its argument to evaluate to a char-set."
                        {:type :arg-type, :expr expr,
                         :culprits {0 (first (:args expr))}}))))
     (mapcat check-expr-arg-type (:args expr)))))

(defn check-arg-type
  "Checks that all operations are given the correct argument type. Requires that
  the :result metadata key is attached to all expressions, e.g. by invoking
  `smallex.reductions/add-arg-results`. Returns a lazy seq of the erroneous
  calls as ExceptionInfos."
  [grammar]
  (mapcat check-expr-arg-type
          (concat (vals (:aliases grammar))
                  (vals (:rules grammar)))))

(defn- check-expr-symbol-refs
  "Recursively checks for symbols not referencing aliases, and returns a lazy
  seq of ExceptionInfos of culprits."
  [g expr]
  (cond (and (= :symbol (:type expr))
             (not (contains? (:alises g) (:value expr))))
        (if (contains? (:rules g) (:value expr))
          (list (ex-info "Symbol cannot refer to a rule definition, must be an alias."
                         {:type :symbol-ref, :expr expr}))
          (list (ex-info "Couldn't find the definition for symbol."
                         {:type :symbol-ref, :expr expr})))
        ;; ^^ TODO: Damerau-Levensthein? =)
        (and (= :op (:type expr))
             (not (-> expr meta :alias-expansion)))
        (mapcat #(check-expr-symbol-refs g %) (:args expr))
        :else nil))

(defn check-symbol-references
  "Checks that all symbols refer to aliases, and creates understandable error
  messages for those which doesn't. Returns a lazy seq of all non-alias
  references as ExceptionInfos. Does not require the aliases to be expanded."
  [grammar]
  (mapcat #(check-expr-symbol-refs grammar %)
          (concat (vals (:aliases grammar))
                  (vals (:rules grammar)))))
