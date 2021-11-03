(ns clojure-empregos-brasil.main
  (:require [clojure.string :as string]
            [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [selmer.parser :as selmer]

            [clojure-empregos-brasil.scrap :as scrap]
            [clojure-empregos-brasil.companies :as companies]
            [clojure-empregos-brasil.markdown :as markdown]))

(def config
  {:jobs-file            "jobs.edn"
   :companies-file       "companies.edn"
   :scraped-jobs-file    "scraped-jobs.edn"
   :target-markdown-file "README.md"})

(defn scrap-companies
  []
  (scrap/scrap-all companies/nubank
                   companies/paygo
                   companies/embraer
                   companies/pipo-saude
                   companies/i80-seguros))

;; TODO: Most of these functions should go into different
;; namespaces.

(defn read-edn-from-filename
  [filename]
  (-> filename
      io/reader
      java.io.PushbackReader.
      edn/read))

(defn read-hardcoded-positions
  []
  (read-edn-from-filename (:jobs-file config)))

(defn read-hardcoded-companies
  []
  (read-edn-from-filename (:companies-file config)))

(defn write-positions
  [positions]
  (spit (:scraped-jobs-file config) (pr-str positions)))

(defn scape-markdown-pipe
  [s]
  (string/replace s #"\|" "&#124;"))

(defn positions-and-companies
  [positions companies]
  {:positions (sort-by (juxt :name :title) positions)
   :companies (group-by :situation companies)})

(def boolean->ptbr
  {true "Sim" false "Não"})

(defn header->ptbr
  [header]
  (set/rename-keys header {:title    "Vaga"
                           :name     "Empresa"
                           :location "Local"
                           :remote   "Remoto?"
                           :url      "Onde aplicar"}))

(defn make-positions-table
  [positions]
  (->> positions
       (map #(-> %
                 (select-keys [:title :name :location :remote :url])

                 (update :remote boolean->ptbr)
                 (update :title scape-markdown-pipe)

                 header->ptbr))

       markdown/markdown-table))

(defn make-companies-table
  [companies]
  (->> companies (map markdown/transform-company) markdown/markdown-table))

;; TODO: Remove slurp to make this a pure function
(defn render-positions
  [positions]
  (selmer/render (slurp (str (:target-markdown-file config) ".template")) positions))

(defn -main [& args]
  (let [{:keys [positions companies]} (positions-and-companies (concat (scrap-companies)
                                                                       (read-hardcoded-positions))
                                                               (read-hardcoded-companies))

        {:keys [unavailable eventual-use]} companies]

    (write-positions positions)

    (->> {:vagas        (make-positions-table positions)
          :sem-vagas    (make-companies-table unavailable)
          :uso-eventual (make-companies-table eventual-use)}
         render-positions
         (spit (:target-markdown-file config)))))
