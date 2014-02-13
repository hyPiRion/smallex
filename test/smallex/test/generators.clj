(ns smallex.test.generators
  (:refer-clojure :exclude [alias])
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

(def collection-generators
  (map #(gen/resize 20 %) [string char-set]))

(declare operation)

(defn- operation-generator
  [{:keys [op max-args type-fn such-that]
    :or {max-args 4 such-that (constantly true)}}]
  (fn [alias-gen]
    (gen/sized
     (fn [size]
       (let [smaller-ops (gen/resize (dec size) (operation alias-gen))
             arg-gen (cond-> (conj collection-generators alias-gen)
                             (pos? size)
                             ;; v- hack to get more nested ops
                             (conj smaller-ops smaller-ops smaller-ops))]
         (->> (gen/vector (gen/one-of arg-gen) 1 max-args)
              (gen/fmap
               (fn [args]
                 (with-meta {:type :op, :value op, :args args}
                   {:result (type-fn (map #(-> % meta :result) args))})))
              (gen/such-that such-that)))))))

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
  (fn [alias-gen]
    (gen/sized
     (fn [size]
       (let [op-generators (map #(gen/resize size (% alias-gen))
                                all-op-generators)]
         (gen/one-of op-generators))))))

(defn expression
  [alias-gen]
  (gen/frequency [[8 (operation alias-gen)]
                  [4 (gen/one-of collection-generators)]
                  [1 alias-gen]]))

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

(defn alias
  [prev-aliases]
  (let [alias-gen (if (seq prev-aliases)
                    (gen/elements prev-aliases)
                    string) ;; avoid throwing when empty
        alias-name (gen/such-that #(and (seq %)
                                        (Character/isLetter (first %))
                                        (not-any? (fn [e] (= % (:value e)))
                                                  prev-aliases))
                                  (gen/resize 20 gen/string-alpha-numeric))
        expr (expression alias-gen)]
    (->> (gen/tuple alias-name expr)
         (gen/fmap (fn [[alias-name expr]]
                     {(with-meta {:type :symbol :value alias-name}
                        {:result (-> expr meta :result)})
                      expr})))))

(defn aliases
  [size]
  (if (zero? size)
    (alias nil)
    (gen/bind (gen/resize (dec size)
                          (gen/sized aliases))
              (fn [alias-map]
                (gen/fmap
                 (fn [generated-alias]
                   (merge alias-map generated-alias))
                 (alias (keys alias-map)))))))

(defn rules
  [alias-defs]
  (let [alias-items (keys alias-defs)
        alias-names (set (map :value alias-items))
        alias-gen (if (seq alias-items)
                    (gen/elements alias-items)
                    string) ;; avoid throwing when empty
        rule-name-gen (gen/such-that #(and (seq %)
                                           (Character/isLetter (first %))
                                           (not (contains? alias-names %)))
                                     (gen/resize 20 gen/string-alpha-numeric))]
    (gen/map rule-name-gen alias-gen)))


(deftest ^:examples test-grammar-generation
  (->>
   (gen/bind (gen/sized aliases)
             (fn [alias-defs]
               (gen/hash-map :aliases (gen/return alias-defs)
                             :rules (rules alias-defs))))
   (gen/fmap (fn [{:keys [aliases rules]}]
               {:aliases (into {}
                               (for [[k v] aliases]
                                 [(:value k) v])) ;; TODO: remove this.
                :rules rules}))
   gen/sample
   ppm))
