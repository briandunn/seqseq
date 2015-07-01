; vim: set ft=clojure:
(set-env!
  :source-paths   #{"src"}
  :resource-paths #{"html"}
  :dependencies '[[adzerk/boot-cljs       "0.0-2814-3" :scope "test"]
                  [org.clojure/clojurescript "0.0-3123"             ]
                  [adzerk/boot-cljs-repl  "0.1.10-SNAPSHOT" :scope "test"]
                  [adzerk/boot-reload     "0.2.6"      :scope "test"]
                  [pandeiro/boot-http     "0.6.2"      :scope "test"]
                  [org.clojure/clojure    "1.7.0-RC1"  :scope "provided"]
                  [org.clojure/core.async "0.1.346.0-17112a-alpha" :scope "provided"]
                  [boot-cljs-test/node-runner "0.1.0" :scope "test"]
                  [secretary              "1.2.3"]
                  [org.omcljs/om          "0.8.8"]])

(require
'[adzerk.boot-cljs      :refer [cljs]]
'[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
'[adzerk.boot-reload    :refer [reload]]
'[pandeiro.boot-http    :refer [serve]]
'[boot-cljs-test.node-runner :refer :all]
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

(deftask auto-test []
  (set-env! :source-paths #{"src" "test"})
  (comp (watch)
        (cljs-test-node-runner :namespaces '[seqseq.test])
        (cljs :source-map true
              :optimizations :none)
        (run-cljs-test)))
