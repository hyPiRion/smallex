(ns smallex.records
  (:require [clojure.string :as s]))

(defn- escaped ^String [^String s]
  (->> (seq s)
       (map #(if (or (Character/isISOControl %)
                     (<= 0x80 (int %) 0xFF))
               (format "\\x%02X" (int %))
               %))
       (map #(if (and (char? %) (< 0xFF (int %)))
               (str "\\u%04X" (int %))
               %))
       (map #({\\ "\\\\" \[ "\\[" \] "\\]" \" "\\\"" \; "\\;"
               \n "\\n" \t "\\t" \r "\\r"} % %))
       (s/join)))

(defmulti ^:private append-type
  (fn [buf elt] (:type elt)))

(defmethod append-type :string
  [buf elt]
  (.append buf \")
  (.append buf (escaped (:value elt)))
  (.append buf \"))

(defmethod append-type :char-set
  [buf elt]
  (.append buf \[)
  (.append buf (escaped (:value elt)))
  (.append buf \]))

(defmethod append-type :op
  [buf elt]
  (.append buf \()
  (.append buf ^String (-> elt :value name))
  (doseq [arg (:args elt)]
    (.append buf \space)
    (append-type buf arg))
  (.append buf \)))

(defmethod append-type :symbol
  [buf elt]
  (.append buf ^String (:value elt)))

(defn- aliases-str
  [buf aliases]
  (doseq [[a-name a-def] aliases]
    (.append buf "(alias ")
    (.append buf a-name)
    (.append buf \space)
    (append-type buf a-def)
    (.append buf ")\n")))

(defn- rules-str
  [buf rules]
  (doseq [[r-name r-def] (sort-by (fn [[k v]] (-> v meta :priority)) rules)]
    (.append buf "(def ")
    (.append buf r-name)
    (.append buf \space)
    (append-type buf r-def)
    (.append buf ")\n")))

(defrecord Grammar [rules aliases]
  Object
  (toString [this]
    (let [buf (StringBuffer.)]
      (aliases-str buf aliases)
      (.append buf \newline)
      (rules-str buf rules)
      (str buf))))

(defrecord Operation [type value args])
