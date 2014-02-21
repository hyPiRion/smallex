(ns smallex.test.reductions
  (:require [clojure.test :refer :all]
            [smallex.test.test-utils :as tu]
            [smallex.reductions :as r])
  (:import (clojure.lang ExceptionInfo)))

(deftest test-alias-expansion
  (testing "that aliases expand as intended"
    (let [g (tu/strs->grammar "(alias foo (or \"true\" \"false\"))"
                              "(def bar (cat foo foo))")
          foo-expr (get-in g [:aliases "foo"])]
      (= (r/expand-aliases g)
         {:rules {"bar" {:type :op, :value :cat, :args [foo-expr foo-expr]}}
          :aliases {"foo" foo-expr}}))))

(deftest test-throws-circular-exeption
  (testing "that circular alias expansions throws"
    (let [g (tu/strs->grammar "(alias foo (cat [a] (or \"\" foo)))"
                              "(def bar foo)")]
      (is (thrown? ExceptionInfo (r/expand-aliases g)))
      (is (try (r/expand-aliases g)
               false
               (catch ExceptionInfo ei
                 (let [data (ex-data ei)]
                   (and (re-find #"cycle" (.getMessage ei))
                        (= (:type data) :cycle)
                        (= (:cycle data) ["foo" "foo"])))))))))
