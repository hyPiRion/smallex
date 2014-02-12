(ns smallex.test.parser.smlx
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [smallex.parser.smlx :as smlx])
  (:import (com.hypirion.smallex SMLXLexer)
           (clojure.lang ExceptionInfo)))

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
