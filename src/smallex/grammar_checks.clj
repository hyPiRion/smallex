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
  lazy seq of the erroneous calls as maps."
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
  "Checks that all operations have the correct arity. Returns a sequence of
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
