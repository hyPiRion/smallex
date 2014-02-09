(ns smallex.main
  (:require [smallex.parse-args :as arg])
  (:import (com.hypirion.smallex SMLXLexer))
  (:gen-class))

(defn -main [& args]
  (let [[argmap remainder] (arg/parse-args args)]
    (prn argmap)
    (prn remainder)
    (prn (->> (SMLXLexer. *in*) iterator-seq (take 5)))))
