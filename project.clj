(defproject smallex "0.1.0-SNAPSHOT"
  :description "TODO: Add in description."
  :url "https://github.com/hyPiRion/smallex"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src" "lexer/clj"]
  :java-source-paths ["lexer/java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :prep-tasks ^:replace [["compile" "#\"com.\""]
                         "javac" "compile"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.3.1"]]
  :profiles {:dev {:plugins [[codox "0.6.4"]]
                   :codox {:output-dir "codox"}}
             :uberjar {:aot :all}}
  :deploy-branches ["stable"]
  :main smallex.main)
