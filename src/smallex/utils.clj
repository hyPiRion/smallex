(ns smallex.utils
  (:require [clojure.java.io :as io])
  (:import (java.io File IOException)))

(def root-dir (System/getProperty "user.dir"))
(def ^:dynamic *cwd* root-dir)

(defn file
  "Returns a file. If the file is relative, it is relative to the current
  working directory (*cwd*)."
  [fname]
  (if (.isAbsolute (File. fname))
    (File. fname)
    (io/file *cwd* fname)))

(defn smallex-version []
  (with-open [reader (-> "META-INF/maven/smallex/smallex/pom.properties"
                         io/resource
                         io/reader)]
    (-> (doto (java.util.Properties.)
          (.load reader))
        (.getProperty "version"))))

(defn user-agent []
  (format "Smallex %s (Java %s; %s %s; %s)"
          (smallex-version) (System/getProperty "java.vm.name")
          (System/getProperty "os.name") (System/getProperty "os.version")
          (System/getProperty "os.arch")))

(defn- some-io*
  "Returns a partially applied function, which returns nil if the single-arity
  function f throws an IOException."
  [f]
  (fn [fname]
    (try
      (f fname)
      (catch IOException _ nil))))

(def
  ^{:arglists '([fname])
    :doc "Return the writer returned by calling
  (clojure.io/writer (utils/file fname)), or nil if the call throws an
  IOException"}
  some-writer
  (some-io* (comp io/writer file)))

(def
  ^{:arglists '([fname])
    :doc "Return the writer returned by calling
  (clojure.io/reader (utils/file fname)), or nil if the call throws an
  IOException"}
  some-reader
  (some-io* (comp io/reader file)))

(defn supported-lang? [lang]
  false)

(defn aside
  "Takes a sequence of functions and returns a fn that takes a single value, a
  sequence of values. The return value is a vector where the first function is
  applied to the first element, the second function to the second element, and
  so on. The sequence will be padded with nils, if there aren't enough
  elements in the vector.
  ((aside f g h) [x y z]) => [(f x) (g y) (h z)]"
  ([f] (fn [[x]] [(f x)]))
  ([f g] (fn [[x y]] [(f x) (g y)]))
  ([f g h] (fn [[x y z]] [(f x) (g y) (h z)]))
  ([f g h & fs]
     (let [fs (list* f g h fs)]
       (fn [args]
         (mapv #(%1 %2) fs args)))))

(defn map-vals
  "Takes a map m and a function f, and applies f to all values in m."
  [m f]
  (->> (seq m)
       (map (aside identity f))
       (into {})))
