(ns smallex.utils
  (:require [clojure.io :as io]
            [clojure.java.io :as io])
  (:import (java.io IOException)))

(def root-dir (System/getProperty "user.dir"))
(def ^:dynamic *cwd* root-dir)

(defn file
  "Returns a file, relative to the current working directory (*cwd*)."
  [fname]
  (io/file *cwd* fname))

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
      (catch (IOException ioe) nil))))

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
