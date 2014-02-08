;; Namespace only used to generate a record.
(ns com.hypirion.smallex
  (:import (clojure.lang Keyword)))

(defrecord Item [^Keyword type ^String value])
