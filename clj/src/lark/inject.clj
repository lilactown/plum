(ns lark.inject
  (:gen-class)
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as node]
            [lark.util :as util]))

(defn result [v tag loc a]
  {:value v
   :tag tag
   :alias a
   :location loc})

(defn inject [alias location]
  (let [aliases (:aliases (util/user-deps-edn))]
    (if-not (some? (aliases alias))
      (result nil ::does-not-exist location alias)
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
                  alias))))))

(comment
  (inject :lark/new "/Users/will/.clojure/deps.edn"))

(defmulti inject-command* :tag)

(defmethod inject-command* ::exists
  [_]
  (println "Alias already exists. Nothing to do!"))

(defmethod inject-command* ::exists-but-different
  [{:keys [value location]}]
  (println "Alias already exists, but doesn't match latest version. Replace? (Y/n)")
  (case (read-line)
    ("Y" "y" "") (do (println "Replacing with latest version.")
                     (spit location (util/prettify value))
                     (println "Success!"))
    ("N" "n") (println "Aborting!")))

(defmethod inject-command* ::success
  [{:keys [value location alias]}]
  (println (str "Adding new alias " alias ". This will modify your deps.edn. OK? (Y/n)"))
  (case (read-line)
    ("Y" "y" "") (do (println "Adding alias.")
                     (spit location (util/prettify value))
                     (println "Success!"))
    ("N" "n") (println "Aborting!")))

(defmethod inject-command* ::does-not-exist
  [{:keys [alias]}]
  (println (str "Alias " alias " was not found.")))

(comment
  (inject-command* {:tag ::exists-but-different :value "asdf" :location "jkl"}))

(defn inject-command [flags alias location]
  (inject-command* (inject alias location)))

(defn -main [& args]
  (inject-command {} (keyword "lark" (first args)) "deps.edn"))
