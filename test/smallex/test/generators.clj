(ns smallex.test.generators
  (:require [simple-check.generators :as gen]
            [clojure.test :refer :all]))

(defn- set->char-set [s]
  {:type :char-set :value (apply str s)})

(def char-set
  (gen/fmap (comp set->char-set set) (gen/vector gen/char)))

(defn- string->item-string [s]
  {:type :string :value s})

(def string
  (gen/fmap string->item-string
            (gen/not-empty gen/string-alpha-numeric)))

(defn use-alias [aliases]
  (gen/elements aliases))

(def op-type
  (gen/elements [:or :cat :star :plus :opt :not]))

(defn operation
  [atom-generators]
  (fn op [size]
    (let [smaller-operation (gen/resize (dec size) (gen/sized op))
          args (cond-> atom-generators
                       (pos? size) (conj smaller-operation))]
      (gen/fmap
       (fn [[op args]]
         {:type :op, :value op :args args})
       (gen/tuple op-type
                  (gen/vector (gen/one-of args) 1 4))))))
