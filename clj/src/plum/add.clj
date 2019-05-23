(ns plum.add
  (:gen-class)
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as node]
            [clojure.string :as s]
            [plum.util :as util]))


(defn determine-type [{:keys [package version]}]
  (cond
    (s/starts-with? package "https://") :git
    (s/starts-with? package "/") :local
    :else :mvn))

(defmulti dep-type #'determine-type)

(defmethod dep-type :mvn
  [{:keys [package version]}]
  {:deps/package (symbol package)
   :deps/version {:mvn/version version}})

(comment
  (dep-type {:package 'foo/bar
             :version "10.1.128"})
  )

(defmethod dep-type :git
  [{:keys [package version]}]
  {:deps/package (let [[user repo] (-> package
                                  (s/split #"/")
                                  (->> (take-last 2)))]
                   (symbol user (if (s/ends-with? repo ".git")
                                  (s/replace-first repo #".git" "")
                                  repo)))
   :deps/version {:git/url package
                  :sha version}})

(comment
  (dep-type {:package "https://github.com/cognitect-labs/test-runner"
             :sha "3cb0a9daf1cb746259dc8309b218f9211ad3b33b"})
  )

(defmethod dep-type :local
  [{:keys [package]}]
  {:deps/package (let [[dir-name] (-> package
                                 (s/split #"/")
                                 (->> (take-last 1)))]
              (symbol dir-name))
   :deps/version {:local/root package}})

(comment
  (dep-type {:package "/foo/bar/baz"})
  )


(defn add-dep [{:keys [package version deps-file] :as dep-info}]
  (let [deps-edn (z/of-file deps-file)
        deps-map (or (-> deps-edn
                         (z/find-value z/next :deps)
                         (z/right))
                     (-> (z/assoc deps-edn :deps {})
                         (z/get :deps)))
        dep-type (dep-type dep-info)]
    (if-let [package-version (z/get deps-map (:deps/package dep-type))]
      (-> package-version
          (z/edit merge (:deps/version dep-type))
          (z/root-string)
          (util/prettify))
      (-> deps-map
          (z/assoc (:deps/package dep-type) (:deps/version dep-type))
          ;; make it pretty
          (z/get (:deps/package dep-type))
          (z/left)
          (z/insert-left (node/newlines 1))
          (z/root-string)
          (util/prettify)))))

(comment
  (spit "dummy.edn"
        (add-dep {:package "https://github.com/clojure/clojure.git"
                  :version "new"
                  :deps-file "dummy.edn"})))

(defn -main [& args]
  (let [file "deps.edn"]
    (println (str "Adding dep " (first args) (when (second args) (str " " (second args))) " to " file "."))
    (spit file
          (add-dep {:package (first args)
                    :version (second args)
                    :deps-file file}))
    (println "Success!")))
