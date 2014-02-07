(ns smallex.parse-args
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [smallex.utils :as utils])
  (:import (java.io File)))

(def ^:private cli-opts
  [["-v" "--version"]
   ["-h" "--help"]
   ["-H" "--langhelp LANG" nil
    :default nil
    :validate [utils/supported-lang? "Unsupported language."]]
   [nil "--loglevel LEVEL" nil
    :default :info
    :parse-fn (fn [level]
                (if (every? #(Character/isUpperCase %) level)
                     (-> level s/lower-case keyword)))
    :validate [#(contains? #{:none :info :debug} %)
               "Must be either NONE, INFO or DEBUG."]]
   [nil "--logfile FILE" nil
    :id :logwriter
    :default *out*
    :parse-fn utils/some-writer
    :validate [(comp not nil?) "Illegal file name."]]
   ["-o" "--out DIR" nil
    :id :outdir
    :default (io/file utils/*cwd*)
    :parse-fn utils/file
    :validate [#(or (not (.exists %)) (.isDirectory %))
               "File name is illegal, or is not a directory."]]
   ["-i" "--in FILE" nil
    :default *in*
    :parse-fn utils/some-reader
    :validate [(comp not nil?) "Illegal file name."]]
   [nil "--list-languages"]
   ["-l" "--lang LANGUAGE" nil
    :validate [utils/supported-lang? "Unsupported language."]]])

(defn parse-args
  "Returns a tuple with arguments and additional args. This function will exit
  the JVM if errors are found with an exit code of 1."
  [arg]
  (let [{:keys [options arguments errors]} (parse-opts arg cli-opts
                                                       :in-order true)]
    (when errors
      (println "Smallex encountered following errors when reading arguments:\n")
      (println (s/join \newline errors))
      (System/exit 1))
    [options arguments]))
