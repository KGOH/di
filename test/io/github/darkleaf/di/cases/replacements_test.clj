(ns io.github.darkleaf.di.cases.replacements-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

(t/deftest replacements-fn-test
  (with-open [obj (di/start ::object
                            (fn [ident]
                              (case ident
                                ::object ::stub)))]
    (t/is (= ::stub @obj))))


(defn use-global-config [{jdbc-url "JDBC_URL"
                          env      :env}]
  [::result jdbc-url env])

(t/deftest global-config-test
  (with-open [obj (di/start ::use-global-config
                            {:env       :test
                             "JDBC_URL" "jdbc url"})]
    (t/is (= [::result "jdbc url" :test] @obj))))


(defn dependency [{}]
  ::dependency)

(defn object [{dep ::dependency}]
  [::object dep])

(t/deftest stub-dep-test
  (with-open [obj (di/start ::object)]
    (t/is (= [::object ::dependency] @obj)))
  (with-open [obj (di/start ::object {::dependency ::stub})]
    (t/is (= [::object ::stub] @obj))))


(t/deftest stub-is-not-managed-test
  (let [p    (promise)
        stub (reify di/Stoppable
               (stop [_]
                 (deliver p ::stopped)))
        obj  (di/start ::object
                       {::dependency stub})]
    (di/stop obj)
    (t/is (not (realized? p)))
    (di/stop stub)
    (t/is (realized? p))))


(defn dependency-replacement [{dep ::other-dependency}]
  [::result dep])

(t/deftest ref-test
  (with-open [obj (di/start ::object
                            {::dependency       (di/ref ::dependency-replacement)
                             ::other-dependency ::stub})]
    (t/is (= [::object [::result ::stub]] @obj))))

(t/deftest var-as-ref-test
  (with-open [obj (di/start ::object
                            {::dependency       #'dependency-replacement
                             ::other-dependency ::stub})]
    (t/is (= [::object [::result ::stub]] @obj))))


(t/deftest ref-n-test
  (with-open [obj (di/start ::root
                            {::root (di/ref ::cfg get-in [:a :b :c])
                             ::cfg  {:a {:b {:c ::value}}}})]
    (t/is (= ::value @obj))))


(t/deftest ref-vec-test
  (with-open [obj (di/start ::root
                            {::root (di/ref-vec [::a ::b])
                             ::a    1
                             ::b    2})]
    (t/is (= [1 2] @obj))))

(t/deftest ref-vec-n-test
  (with-open [obj (di/start ::root
                            {::root (di/ref-vec [::a ::b] conj 3)
                             ::a    1
                             ::b    2})]
    (t/is (= [1 2 3] @obj))))


(t/deftest ref-map-test
  (with-open [obj (di/start ::root
                            {::root (di/ref-map #{::a ::b})
                             ::a    1
                             ::b    2})]
    (t/is (= {::a 1, ::b 2} @obj))))

(t/deftest ref-map-n-test
  (with-open [obj (di/start ::root
                            {::root (di/ref-map #{::a ::b} assoc ::c 3)
                             ::a    1
                             ::b    2})]
    (t/is (= {::a 1, ::b 2, ::c 3} @obj))))
