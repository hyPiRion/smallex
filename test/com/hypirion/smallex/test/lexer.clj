(ns com.hypirion.smallex.test.lexer
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]])
  (:use [clojure.test])
  (:import (com.hypirion.smallex SMLXLexer)))

(deftest test-read
  (with-open [rdr (io/reader (io/resource "clojure.smlx"))]
    (is (try
          (doall (iterator-seq (SMLXLexer. rdr)))
          true
          (catch Exception _ false)))))
