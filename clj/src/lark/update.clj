(ns lark.update
  (:gen-class)
  (:require [rewrite-clj.zip :as z]
            [lark.inject :as inject]
            [rewrite-clj.node :as node]
            [lark.util :as util]))

(def aliases
  `{:lark/inject {:extra-deps {lilactown/lark {:local/root ~(util/lark-location)}}
                  :main-opts ["-m" "lark.inject"]}
    :lark/update {:extra-deps {lilactown/lark {:local/root ~(util/lark-location)}}
                  :main-opts ["-m" "lark.update"]}
    :lark/add {:extra-deps {lilactown/lark {:local/root ~(util/lark-location)}}
               :main-opts ["-m" "lark.add"]}
    :lark/repl {:extra-deps {com.bhauman/rebel-readline {:mvn/version "0.1.4"}}
                :main-opts  ["-m" "rebel-readline.main"]}
    :lark/new {:extra-deps {seancorfield/clj-new
                            {:mvn/version "0.5.5"}}
               :main-opts ["-m" "clj-new.create"]}
    :lark/deps {:extra-deps {olical/depot {:mvn/version "1.8.2"}}
                :main-opts ["-m" "depot.outdated.main"]}
    :lark/gen {:extra-deps {seancorfield/clj-new
                            {:mvn/version "0.5.5"}}
               :main-opts ["-m" "clj-new.generate"]}
    :lark/test {:extra-paths ["test" "src/test/clojure"]
                :extra-deps {org.clojure/test.check {:mvn/version "RELEASE"}
                             com.cognitect/test-runner
                             {:git/url "https://github.com/cognitect-labs/test-runner"
                              :sha "3cb0a9daf1cb746259dc8309b218f9211ad3b33b"}}
                :main-opts ["-m" "cognitect.test-runner"
                            "-d" "test"
                            "-d" "src/test/clojure"]}})

(defn install-aliases [deps-aliases]
  (-> (loop [deps-aliases deps-aliases
             alias-keys (keys aliases)]
        ;; (println (z/string deps-aliases))
        (if-some [al (first alias-keys)]
          (recur (-> deps-aliases
                     (z/assoc al (aliases al))
                     (z/get al)
                     (z/left)
                     (z/insert-left (node/newlines 1))
                     (z/up))
                 (next alias-keys))
          deps-aliases))
      (z/root-string)
      (util/prettify)))

(defn -main [& args]
  (let [file (util/user-deps-location)
        deps-edn (z/of-file file)
        deps-aliases (or (-> deps-edn
                             (z/find-value z/next :aliases)
                             (z/right))
                         (-> (z/assoc deps-edn :aliases {})
                             (z/get :aliases)))]
    (println (str "Backing up " file " to " (str file ".bak") "."))
    (spit (str file ".bak") (slurp file))
    (println (str "Installing aliases into " file "."))
    (spit file (install-aliases deps-aliases))
    (println "Success!")))

(comment
  (-main))
