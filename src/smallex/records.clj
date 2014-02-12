(ns smallex.records)

(defrecord Grammar [rules aliases])

(defrecord Operation [type value args ^long line ^long col])
