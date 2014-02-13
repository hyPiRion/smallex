(ns smallex.test.generators
  (:require [simple-check.generators :as gen]
            [clojure.test :refer :all]
            [clojure.pprint :as pprint :refer [pprint]]))

(defn- set->char-set [s]
  (with-meta
    {:type :char-set :value (apply str s)}
    {:result :char}))

(def char-set
  (gen/fmap (comp set->char-set set) (gen/vector gen/char)))

(defn- string->item-string [s]
  (with-meta
    {:type :string :value s}
    {:result (if (= 1 (count s))
               :char
               :string)}))

(def string
  (gen/fmap string->item-string
            (gen/not-empty gen/string-alpha-numeric)))

(defn use-alias [aliases]
  (gen/elements aliases))

(def collection-generators [string char-set])

(declare operation)

(defn- operation-generator
  [{:keys [op max-args type-fn such-that]
    :or {max-args 4 such-that (constantly true)}}]
  (gen/sized
   (fn [size]
     (let [smaller-ops (gen/resize (dec size) operation)
           arg-gen (cond-> (map #(gen/resize 20 %) collection-generators)
                           (pos? size) (conj smaller-ops))]
       (->> (gen/vector (gen/one-of arg-gen) 1 max-args)
            (gen/fmap
             (fn [args]
               (with-meta {:type :op, :value op, :args args}
                 {:result (type-fn (map #(-> % meta :result) args))})))
            (gen/such-that such-that))))))

(def or-op
  (operation-generator
   {:op :or
    :type-fn (fn [args]
               (if (every? #(= :char %) args)
                 :char
                 :string))}))

(def cat-op
  (operation-generator
   {:op :cat
    :type-fn (fn [[f & r]]
               (if (seq r)
                 :string
                 f))}))
(def star-op
  (operation-generator
   {:op :star, :max-args 1, :type-fn (constantly :string)}))

(def plus-op
  (operation-generator
   {:op :plus, :max-args 1, :type-fn (constantly :string)}))

(def opt-op
  (operation-generator
   {:op :opt, :max-args 1, :type-fn (constantly :string)}))

(def not-op
  (operation-generator
   {:op :not, :max-args 1, :type-fn (fn [[f]] f)
    :such-that #(-> % meta :result (= :char))}))

(def all-op-generators
  [or-op cat-op star-op plus-op opt-op not-op])

(def operation
  (gen/sized
   (fn [size]
     (let [op-generators (map #(gen/resize size %) all-op-generators)]
       (gen/one-of op-generators)))))

(defn ppm [obj]
  (let [orig-dispatch pprint/*print-pprint-dispatch*]
    (pprint/with-pprint-dispatch
      (fn [o]
        (when (meta o)
          (print "^")
          (orig-dispatch (meta o))
          (pprint/pprint-newline :fill))
        (orig-dispatch o))
      (pprint obj))))

(deftest ^:examples print-op-result
  (ppm (gen/sample operation)))
