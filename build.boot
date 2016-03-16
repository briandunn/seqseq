; vim: set ft=clojure:
(set-env!
  :source-paths   #{"src"}
  :resource-paths #{"html"}
  :dependencies '[[adzerk/boot-cljs                "1.7.228-1" :scope "test"]
                  [adzerk/boot-cljs-repl           "0.3.0"     :scope "test"]
                  [com.cemerick/piggieback         "0.2.1"     :scope "test"]
                  [weasel                          "0.7.0"     :scope "test"]
                  [org.clojure/tools.nrepl         "0.2.12"    :scope "test"]
                  [org.clojure/clojurescript       "1.7.228"   :scope "test"]
                  [adzerk/boot-reload              "0.2.6"     :scope "test"]
                  [pandeiro/boot-http              "0.7.3"     :scope "test"]
                  [adzerk/boot-test                "1.0.4"     :scope "test"]
                  [ring/ring-devel                 "1.3.2"     :scope "test"]
                  [com.joshuadavey/boot-middleman  "0.0.4"     :scope "test"]
                  [org.clojure/core.async          "0.2.374"   :scope "provided"]
                  [org.clojure/clojure             "1.8.0"     :scope "provided"]
                  [re-frame                        "0.5.0"]
                  [secretary                       "1.2.3"]
                  [prismatic/schema                "0.4.3"]])

(require
  '[adzerk.boot-cljs      :refer [cljs]]
  '[adzerk.boot-test]
  '[com.joshuadavey.boot-middleman :refer [middleman]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload    :refer [reload]]
  '[pandeiro.boot-http    :refer [serve]])

(deftask dev []
  (comp
    (serve :not-found 'seqseq.history-handler/app :reload true :dir "target")
    (watch)
    (middleman)
    (reload :on-jsreload 'seqseq.main/init)
    (speak)
    (cljs-repl)
    (cljs :source-map true)))

(deftask test []
  (set-env! :source-paths #{"test" "src"})
  (comp  (speak) (watch) (adzerk.boot-test/test)))
