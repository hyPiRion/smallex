(ns smallex.test.parser.smlx
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [smallex.parser.smlx :as smlx])
  (:import (com.hypirion.smallex SMLXLexer)
           (clojure.lang ExceptionInfo)))

(deftest test-clj-spec
  (with-open [rdr (io/reader (io/resource "clojure.smlx"))]
    (let [item-seq (iterator-seq (SMLXLexer. rdr))
          grammar (smlx/parse item-seq)]
      (testing "'def' count in legal grammar should be equal to rule count"
          (is (= (count (:rules grammar))
                 (count (filter #(= :def (:type %)) item-seq)))))
      (testing "'alias' count in legal grammar should be equal to alias count"
        (is (= (count (:aliases grammar))
               (count (filter #(= :alias (:type %)) item-seq))))))))
