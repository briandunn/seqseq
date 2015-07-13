; vim: set ft=clojure:
(set-env!
  :source-paths   #{"src"}
  :resource-paths #{"html"}
  :dependencies '[[adzerk/boot-cljs       "0.0-2814-3"             :scope "test"]
                  [adzerk/boot-cljs-repl  "0.1.9"                  :scope "test"]
                  [org.clojure/clojurescript "0.0-2816"            :scope "test"]
                  [adzerk/boot-reload     "0.2.6"                  :scope "test"]
                  [pandeiro/boot-http     "0.6.2"                  :scope "test"]
                  [com.cemerick/clojurescript.test  "0.3.3"        :scope "test"]
                  [org.clojure/core.async "0.1.346.0-17112a-alpha" :scope "provided"]
                  [org.clojure/clojure    "1.7.0-RC1"              :scope "provided"]
                  [secretary "1.2.3"]
                  [reagent   "0.5.0"]])

(require
'[adzerk.boot-cljs      :refer [cljs]]
'[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
'[adzerk.boot-reload    :refer [reload]]
'[pandeiro.boot-http    :refer [serve]]
)

(deftask dev []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (serve :dir "target/")
    (watch)
    (reload)
    (speak)
    (cljs-repl)
    (cljs :unified true
          :source-map true)))

(deftask test-repl []
  (comp (repl :client true)))
