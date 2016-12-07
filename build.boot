(def project 'manifold-cljs/core.async)
(def version "0.1.6-0")

(set-env! :resource-paths #{"src"}
          :dependencies   '[[org.clojure/clojure "1.8.0" :scope "provided"]
                            [org.clojure/clojurescript "1.9.293" :scope "provided"]
                            [adzerk/boot-cljs "1.7.228-2" :scope "test"
                             :exclusions [org.clojure/clojurescript]]
                            [crisptrutski/boot-cljs-test "0.2.2-SNAPSHOT" :scope "test"]
                            [adzerk/bootlaces "0.1.13" :scope "test"]

                            [manifold-cljs "0.1.6-0"]
                            [org.clojure/core.async "0.2.395"]])

(task-options!
 pom {:project     project
      :version     version
      :description "Core.Async stream adapter for Manifold-Cljs"
      :url         "https://github.com/dm3/manifold-cljs.core.async"
      :scm         {:url "https://github.com/dm3/manifold-cljs.core.async"}
      :license     {"MIT License" "https://opensource.org/licenses/MIT"}})

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.bootlaces :as l :refer [push-release]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]])

(l/bootlaces! version :dont-modify-paths? true)

(deftask build-jar []
  (comp (pom) (jar) (install)))

(deftask release []
  (comp (build-jar) (push-release)))

(defn dev! []
  (task-options! cljs {:optimizations :none, :source-map true}))

(deftask dev []
  (dev!)
  (comp (watch)
        (cljs)))

(deftask test []
  (merge-env! :resource-paths #{"test"})
  (dev!)
  (test-cljs))

(deftask autotest []
  (merge-env! :resource-paths #{"test"})
  (dev!)
  (comp (watch)
        (test-cljs)))
