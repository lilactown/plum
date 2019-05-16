(ns lark.util
  (:require [cljfmt.core :as cljfmt]))

(defn prettify [string]
  (cljfmt/reformat-string string {:ansi?        true
                                  :indentation? true
                                  :insert-missing-whitespace?      true
                                  :remove-surrounding-whitespace?  true
                                  :remove-trailing-whitespace?     true
                                  :remove-consecutive-blank-lines? true
                                  :indents   cljfmt/default-indents
                                  :alias-map {}}))

(defn user-deps-edn []
  (read-string (slurp (str (System/getProperty "user.home")
                           "/.clojure/deps.edn"))))
