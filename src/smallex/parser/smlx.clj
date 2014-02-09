(ns smallex.parser.smlx
  (:require [smallex.records :as r])
  (:import (smallex.records Grammar Expression)))

(declare parse-root parse-definition parse-expr parse-operation parse-args
         end-paren should-exist)

(defn parse
  "Parses the items from a smlx seq (maps having :type and :value) to a proper
  Grammar. Does not perform alias expansion, nor checks that the semantic rules
  are correct."
  [item-seq]
  (loop [item-seq item-seq
         grammar (r/map->Grammar {:rules {}, :aliases {}})]
    (if (empty? item-seq)
      grammar
      (let [[item-seq* updated-grammar] (parse-root item-seq grammar)]
        (recur item-seq* updated-grammar)))))

(defn- parse-root
  "Returns a vector [item-seq grammar], where item-seq is the remaining
  arguments and the grammar is the updated grammar after parsing the next
  toplevel expression."
  [[paren def-or-alias & r] grammar]
  (should-exist paren)
  (cond (not= (:type paren) :paren-start)
        (throw (ex-info "Expected a start paren at root." paren))
        (distinct? (:type def-or-alias) :def :alias)
        (throw (ex-info "Expected either \"def\" or \"alias\"." def-or-alias))
        :else
        (parse-definition def-or-alias r grammar)))

(defn- grammar-key
  "Returns the key in which to assoc a definition into the grammar, given the
  definition item."
  [item]
  ({:def :rules, :alias :aliases} (:type item)))

(defn- parse-definition
  "Parses a definition ('alias' or 'def'), and inserts it into the grammar.
  Returns the vector [item-seq grammar], where the item-seq is the remaining
  items, and grammar is the updated grammar."
  [definition-type [name-item & item-seq] grammar]
  (should-exist name-item)
  (if (some #(contains? % (:value name-item)) (vals grammar))
    (throw (ex-info "Definition already exists." name-item))
    (let [[rem-seq expr] (end-paren parse-expr item-seq)
          updated-grammar (assoc grammar (grammar-key definition-type)
                                 (:value name-item) expr)]
      [rem-seq updated-grammar])))

(defn- parse-expr
  "Parses a single expression, which could be a paren-expression, a string
  type or a possible alias (symbol)."
  [[head-item & item-seq]]
  (should-exist head-item)
  (cond (contains? #{:string :char-set :symbol} (:type head-item))
        [item-seq head-item]
        (= (:type head-item) :paren-start)
        (end-paren parse-operation head-item item-seq)))

(defn- parse-operation
  "Parses an operation, which is some known operator along with several
  arguments delimited by a closing paren."
  [op item-seq]
  (should-exist op)
  (if-not (= (:type op) :op)
    (throw (ex-info "Expected an operation." op))
    (let [[rem-seq args] (parse-args item-seq [])]
      ;; Should we retain op item here? Sounds reasonable to at least have the
      ;; position it was defined for better error msg later.
      [rem-seq (r/map->Expression {:op (:value op), :args args})])))

(defn- parse-args
  "Parses legal expressions until a closing paren is found, then returning
  [rem-seq args]."
  [[head :as item-seq] args]
  (should-exist head)
  (if (= (:type head) :paren-end)
    [(rest item-seq) args]
    (let [[rem-seq expr] (parse-expr item-seq)]
      (recur rem-seq (conj args expr)))))

(defn- end-paren
  "Call a function f, which returns [item-seq x]. If the first element in
  item-seq is not a closing paren, will throw an appropriate error. Otherwise
  returns [(rest item-seq] x]."
  [f & args]
  (let [[[paren & rem-seq] x] (apply f args)]
    (should-exist paren)
    (if (= (:type paren) :paren-end)
      [rem-seq x]
      (throw (ex-info "Expected a closing paren." paren)))))

(defn- should-exist
  "Checks whether the item actually exists, and if not, throws an appropriate
  error. As the error item is never expected, it will throw an error if the item
  is an error item."
  [item]
  (cond (nil? item)
        (throw (ex-info "Unexpected end of grammar." {}))
        (= (:type item) :error)
        (throw (ex-info "Lexer returned an error item." item))))
