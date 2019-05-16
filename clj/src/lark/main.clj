(ns lark.main
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [rewrite-clj.zip :as z]
            [rewrite-clj.node :as node]
            [cljfmt.core :as cljfmt]))

(def aliases
  '{:lark {:extra-deps {lilactown/lark {:local/root "/Users/will/Code/lark/clj/"}}}
    :lark/repl {:extra-deps {com.bhauman/rebel-readline {:mvn/version "0.1.4"}}
                :main-opts  ["-m" "rebel-readline.main"]}
    :lark/new {:extra-deps {seancorfield/clj-new
                            {:mvn/version "0.5.5"}}
               :main-opts ["-m" "clj-new.create"]}})

(def cli-options
  [["-i" "--inject alias" "Injects specified alias into a deps.edn"]])


(defn prettify [string]
  (cljfmt/reformat-string string {:ansi?        true
                                  :indentation? true
                                  :insert-missing-whitespace?      true
                                  :remove-surrounding-whitespace?  true
                                  :remove-trailing-whitespace?     true
                                  :remove-consecutive-blank-lines? true
                                  :indents   cljfmt/default-indents
                                  :alias-map {}}))

;;
;; Injection
;;

(comment
  (def deps-edn (z/of-file "/Users/will/.clojure/deps.edn"))

  (def aliases (z/right (z/find-value deps-edn z/next :aliases)))

  (z/string (z/get aliases :foo))

  (def new-aliases (z/assoc aliases :foo "bar"))

  (read-string (z/root-string new-aliases))

  (def dummy (z/of-file "dummy.edn"))

  (z/right (z/find-value dummy z/next :aliases))

  (z/string (z/get (z/assoc dummy :aliases {}) :aliases))
  )

(defn result [v tag loc a]
  {:value v
   :tag tag
   :alias a
   :location loc})

(defn inject [alias location]
  (let [deps-edn (z/of-file location)
        deps-aliases (or (-> deps-edn
                             (z/find-value z/next :aliases)
                             (z/right))
                         (-> (z/assoc deps-edn :aliases {})
                             (z/get :aliases)))]
    (if-let [alias-v (z/get deps-aliases alias)]
      ;; alias exists
      (if (= (read-string (z/string alias-v)) (aliases alias))
        ;; already injected
        (result nil ::exists location alias)
        ;; exists but different. maybe old version?
        (result (-> deps-aliases
                    (z/assoc alias (aliases alias))
                    (z/get alias)
                    (z/left)
                    (z/insert-left (node/newlines 1))
                    (z/root-string))
                ::exists-but-different
                location
                alias))
      ;; alias doesn't exist. inject it
      (result (-> deps-aliases
                  (z/assoc alias (aliases alias))
                  (z/get alias)
                  (z/left)
                  (z/insert-left (node/newlines 1))
                  (z/root-string))
              ::success
              location
              alias))))

(comment
  (inject :lark/new "/Users/will/.clojure/deps.edn")
  )

(defmulti inject-command* :tag)

(defmethod inject-command* ::exists
  [_]
  (println "Alias already exists. Nothing to do!"))

(defmethod inject-command* ::exists-but-different
  [{:keys [value location]}]
  (println "Alias already exists, but doesn't match latest versoin. Replace? (Y/n)")
  (case (read-line)
    ("Y" "y" "") (do (println "Replacing with latest version...")
                  (spit location (prettify value))
                  (println "Success!"))
    ("N" "n") (println "Aborting!")))

(defmethod inject-command* ::success
  [{:keys [value location alias]}]
  (println (str "Adding new alias " alias ". This will modify your deps.edn. OK? (Y/n)"))
  (case (read-line)
    ("Y" "y" "") (do (println "Adding alias...")
                     (spit location (prettify value))
                     (println "Success!"))
    ("N" "n") (println "Aborting!")))

(comment
  (inject-command* {:tag ::exists-but-different :value "asdf" :location "jkl"}))

(defn inject-command [alias location]
  (inject-command* (inject alias location)))


(comment
  (inject-command :lark "dummy.edn")
  (inject-command :lark/repl "dummy.edn")
  (inject-command :lark/new "dummy.edn")
  )

(defn -main [& args]
  (let [{:keys [options arguments errors]} (parse-opts args cli-options)]
    (cond
      (:inject options) (inject-command :lark "dummy.edn"))))
