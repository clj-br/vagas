(ns clojure-empregos-brasil.markdown
  (:require [clojure.string :as string]
            [clojure.pprint :as pprint]
            [clojure.set :as set]))

(defn markdown-table
  "Build a markdown table in rows"
  [rows]
  (string/replace (with-out-str
                    (pprint/print-table rows))
                  #"\+" "|"))

(defn transform-company
  "Select :name :website and :apply keys and rename them to translated
   strings"
  [company]
  (-> company
      (select-keys [:name :website :apply])
      (set/rename-keys {:name    "Empresa"
                        :website "Site"
                        :apply   "Onde aplicar"})))
