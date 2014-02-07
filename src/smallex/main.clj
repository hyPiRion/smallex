(ns smallex.main
  (:require [smallex.parse-args :as arg])
  (:gen-class))

(defn -main [& args]
  (let [[argmap remainder] (arg/parse-args args)]
    (prn argmap)
    (prn remainder)))
