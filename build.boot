; vim: set ft=clojure:
(set-env!
  :source-paths   #{"src"}
  :resource-paths #{"html"}
  :dependencies '[[adzerk/boot-cljs          "0.0-2814-3"             :scope "test"]
                  [adzerk/boot-cljs-repl     "0.1.9"                  :scope "test"]
                  [org.clojure/clojurescript "0.0-3308"               :scope "test"]
                  [adzerk/boot-reload        "0.2.6"                  :scope "test"]
                  [pandeiro/boot-http        "0.6.2"                  :scope "test"]
                  [org.clojure/core.async    "0.1.346.0-17112a-alpha" :scope "provided"]
                  [org.clojure/clojure       "1.7.0"                  :scope "provided"]
                  [re-frame                  "0.4.1"]
                  [adzerk/boot-test          "1.0.4"]
                  [secretary                 "1.2.3"]
                  [prismatic/schema          "0.4.3"]
                  [reagent                   "0.5.0"]])

(require
  '[adzerk.boot-cljs      :refer [cljs]]
  '[adzerk.boot-test]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload    :refer [reload]]
  '[pandeiro.boot-http    :refer [serve]])

(deftask dev []
  (comp
    (serve :dir "target/")
    (watch)
    (reload :on-jsreload 'seqseq.main/init)
    (speak)
    (cljs-repl)
    (cljs :unified true
          :source-map true)))

(deftask test []
  (set-env! :source-paths #{"test" "src"})
  (comp  (speak) (watch) (adzerk.boot-test/test)))
