(ns clojure-empregos-brasil.scrap
  (:require [net.cgrand.enlive-html :as html])
  (:import (java.net URL)))


(defn attr
  "Returns a function that receives a node and returns the
   k attribute of the node."
  [k]
  (fn [node]
    (get (:attrs node) k)))

(defn scrap
  "Scrap all the items that match path in document and map then using mapping.
   Returns a sequence of maps which equivalent to mapping.

   If mapping is something like {:href (attr :href) :label [html/text-node]}
   scrap will return something like:

   > (scrap document [:a] {:href (attr :href) :label [html/text-node]})
   ({:href \"/about\" :label \"About\"})
  "
  [document path mapping]
  (for [opening (html/select document path)]
    (into {}
          (for [[key value-path] mapping]
            [key
             (cond
               (vector? value-path) (first (html/select opening value-path))
               (fn? value-path) (value-path opening)
               :else value-path)]))))

(defn replace-path
  "Replace a path for a new-path in a url"
  [new-path url]
  (let [url (URL. url)]
    (str (URL. (.getProtocol url)
               (.getHost url)
               new-path))))

(defn scrap-all
  "Scrap all companies filtering Clojure brazilian engineers and returning a full
   sequence."
  [& companies]
  (flatten
    (for [{:keys [engineer? brazil? clojure? enrich name page] :as company} companies]
      (let [html (html/html-resource (URL. page))
            positions (scrap html
                             (:path company)
                             (:scrap company))]
        (->> positions
             (filter #(and (brazil? %) (clojure? %) (engineer? %)))
             (map #(assoc % :name name
                            :url (replace-path (:url %)
                                               (:page company)))))))))
