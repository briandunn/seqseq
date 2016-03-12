(ns seqseq.history-handler
  (:require [ring.util.response :refer [file-response]]))

(defn app [request]
  (assoc-in
    (file-response "target/index.html")
    [:headers "Content-Type"]
    "text/html;charset=utf8"))
