(ns plum.add
  (:gen-class)
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as node]
            [plum.util :as util]))

(defn add-dep [{:keys [package version deps-file]}]
  (let [deps-edn (z/of-file deps-file)
        deps-map (or (-> deps-edn
                         (z/find-value z/next :deps)
                         (z/right))
                     (-> (z/assoc deps-edn :deps {})
                         (z/get :deps)))]
    (if-let [package-version (z/get deps-map package)]
      (-> package-version
          (z/assoc :mvn/version version)
          (z/root-string)
          (util/prettify))
      (-> deps-map
          (z/assoc package {:mvn/version version})
          ;; make it pretty
          (z/get package)
          (z/left)
          (z/insert-left (node/newlines 1))
          (z/root-string)
          (util/prettify)))))

(comment
  (spit "dummy.edn"
        (add-dep {:package (symbol "org.clojure/clojure")
                  :version "1.10.0"
                  :deps-file "dummy.edn"})))

(defn -main [& args]
  (let [file "deps.edn"]
    (println (str "Adding dep " (first args) " {:mvn/version \"" (second args) "\"} to " file "."))
    (spit file
          (add-dep {:package (symbol (first args))
                    :version (second args)
                    :deps-file file}))
    (println "Success!")))
