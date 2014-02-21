(ns smallex.test.test-utils
  (:require [smallex.parser.smlx :as smlx])
  (:import (com.hypirion.smallex SMLXLexer)
           (java.io StringReader)))

(defn strs->grammar
  "Converts strings to a grammar."
  [& strs]
  (-> (apply str strs) StringReader. SMLXLexer. iterator-seq smlx/parse))
