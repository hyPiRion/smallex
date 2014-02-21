(ns smallex.test.parser.smlx
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.test.check.clojure-test :as ct :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [smallex.parser.smlx :as smlx]
            [smallex.test.generators :as s-gen])
  (:import (com.hypirion.smallex SMLXLexer)
           (clojure.lang ExceptionInfo)
           (java.io StringReader)))

(defn- filter-afterv
  "Like filterv, but picks the item after the matched element, if existing."
  [f coll]
  (loop [matched []
         [fst & rst] coll]
    (cond (and (seq rst) (f fst))
          (recur (conj matched (first rst))
                 rst)
          (seq rst)
          (recur matched rst)
          :else
          matched)))

;; Hack to not increase sizedness of tests by too much.
(defmacro ^:private change-max-size
  [name max-size]
  `(alter-meta! (var ~name) assoc
                :test (fn [] (#'ct/assert-check
                             (assoc (~name ct/*default-test-count*
                                           :max-size ~max-size)
                               :test-var (str '~name))))))

(defmacro ^:private deftest-grammar
  [name bind-names & body]
  (let [grammar (gensym "grammar")]
    `(do (defspec ~name
           (prop/for-all [~grammar (gen/fmap str s-gen/grammar)]
             (let [~@(interleave bind-names
                                 `[(-> ~grammar StringReader.
                                       SMLXLexer. iterator-seq)
                                   (smlx/parse ~(first bind-names))])]
               ~@body)))
         (change-max-size ~name 40))))

(deftest-grammar def-count-equal-to-rule-count
  [item-seq grammar]
  (testing "'def' count in legal grammar should be equal to rule count"
    (is (= (count (:rules grammar))
           (count (filter #(= :def (:type %)) item-seq))))))

(deftest-grammar alias-count-equal-to-aliases
  [item-seq grammar]
  (testing "'alias' count in legal grammar should be equal to alias count"
    (is (= (count (:aliases grammar))
           (count (filter #(= :alias (:type %)) item-seq))))))

(deftest-grammar equivalent-aliases
  [item-seq grammar]
  (testing "aliases in grammar and item-seq should be equivalent"
    (is (= (-> grammar :aliases keys set)
           (->> item-seq
                (filter-afterv #(= :alias (:type %)))
                (map :value)
                set)))))

(deftest-grammar equivalent-rules
  [item-seq grammar]
  (testing "rules in grammar and item-seq should be equivalent"
    (is (= (-> grammar :rules keys set)
           (->> item-seq
                (filter-afterv #(= :def (:type %)))
                (map :value)
                set)))))

(deftest-grammar dependent-rule-ordering
  [item-seq grammar]
  (testing "rules in grammar is prioritised by ordering from grammar file"
    (is (= (->> (:rules grammar)
                (sort-by (fn [[_ expr]] (-> expr meta :priority)))
                (map key))
           (->> item-seq
                (filter-afterv #(= :def (:type %)))
                (map :value))))))

(deftest test-clj-spec
  (with-open [rdr (io/reader (io/resource "clojure.smlx"))]
    (let [item-seq (iterator-seq (SMLXLexer. rdr))
          grammar (smlx/parse item-seq)]
      (testing "'def' count in legal grammar should be equal to rule count"
        (is (= (count (:rules grammar))
               (count (filter #(= :def (:type %)) item-seq)))))
      (testing "'alias' count in legal grammar should be equal to alias count"
        (is (= (count (:aliases grammar))
               (count (filter #(= :alias (:type %)) item-seq)))))
      (testing "aliases in grammar and item-seq should be equivalent"
        (is (= (-> grammar :aliases keys set)
               (->> item-seq
                    (filter-afterv #(= :alias (:type %)))
                    (map :value)
                    set))))
      (testing "rules in grammar and item-seq should be equivalent"
        (is (= (-> grammar :rules keys set)
               (->> item-seq
                    (filter-afterv #(= :def (:type %)))
                    (map :value)
                    set))))
      (testing "rules in grammar is prioritised by ordering from grammar file"
        (is (= (->> (:rules grammar)
                    (sort-by (fn [[_ expr]] (-> expr meta :priority)))
                    (map key))
               (->> item-seq
                    (filter-afterv #(= :def (:type %)))
                    (map :value))))))))
