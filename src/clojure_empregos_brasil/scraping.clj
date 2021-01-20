(ns clojure-empregos-brasil.scraping
  (:require [clojure.string :as string]
            [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html]
            [selmer.parser :as selmer]))

(defn attr [x]
  (fn [node]
    (get (:attrs node) x)))

(defn scrap
  [html path mapping]
  (for [opening (html/select html path)]
    (into {}
          (for [[key value-path] mapping]
            [key
             (cond
               (vector? value-path) (first (html/select opening value-path))
               (fn? value-path) (value-path opening)
               :else value-path)]))))

(def nubank
  {:name      "Nubank"
   :page      "https://boards.greenhouse.io/nubank"
   :path      [:div.opening]
   :scrap     {:title      [:a html/text-node]
               :url        #(-> % (html/select [:a]) first :attrs :href)
               :location   [:.location html/text-node]
               :department (attr :department_id)
               :office_id  (attr :office_id)
               :remote     false}
   :engineer? (comp (partial = "60350") :department)
   :brazil?   (comp (partial = "58102") :office_id)
   :clojure?  boolean
   :enrich    identity})

(def gupy
  {:title      [:.title html/text-node]
   :url        #(-> % (html/select [:a.job-list__item]) first :attrs :href)
   :location   (attr :data-workplace)
   :department (attr :data-department)
   :remote     (comp (partial = "true") (attr :data-remote))
   :type       (attr :data-type)})

(def paygo
  {:name      "PayGo"
   :page      "https://paygo.gupy.io/"
   :path      [:.job-list :tr]
   :scrap     gupy
   :engineer? #(= (:department %) "Tecnologia")
   :brazil?   boolean
   :clojure?  #(string/includes?
                 (-> % :title string/lower-case)
                 "clojure")})

(def embraer
  {:name      "Embraer"
   :page      "https://embraer.gupy.io/"
   :path      [:.job-list :tr]
   :scrap     gupy
   :engineer? #(= (:department %) "Inovação")
   :brazil?   :remote
   :clojure?  #(string/includes?
                 (-> % :title string/lower-case)
                 "clojure")})

(def pipo-saude
  {:name      "Pipo Saúde"
   :page      "https://pipo-saude.breezy.hr/"
   :path      [:li.position]
   :scrap     {:title      [:h2 html/text-node]
               :url        #(-> % (html/select [:a]) first :attrs :href)
               :location   [:ul.meta :li.location html/text-node]
               :department [:ul.meta :li.department html/text-node]
               :type       [:ul.meta :li.type html/text-node]}
   :engineer? #(= (:department %) "Engenharia")
   :brazil?   boolean
   :clojure?  boolean})

(defn scrap-all [& companies]
  (flatten
    (for [{:keys [engineer? brazil? clojure? enrich name page] :as company} companies]
      (let [html (html/html-resource (java.net.URL. page))
            positions (scrap html
                             (:path company)
                             (:scrap company))]
        (->> positions
             (filter #(and (brazil? %) (clojure? %) (engineer? %)))
             (map #(assoc % :name name
                            :url (str page (:url %)))))))))

(defn markdown-table
  [x]
  (string/replace (with-out-str
                    (pprint/print-table x))
                  #"\+" "|"))

(defn translate-values
  [x]
  (-> x
      (update :remote {true "Sim" false "Não"})
      (update :title #(string/replace % #"\|" "-"))))

(defn transform-company
  [company]
  (-> company
      (select-keys [:name :website :apply])
      (set/rename-keys {:name    "Empresa"
                        :website "Site"
                        :apply   "Onde aplicar"})))

(defn -main [& args]
  (let [positions (scrap-all nubank paygo embraer pipo-saude)
        companies (edn/read (java.io.PushbackReader. (io/reader "companies.edn")))

        {:keys [unavailable eventual-use]} (group-by :situation companies)]
    (spit "scraped-jobs.edn" (pr-str positions))

    (->> {:vagas        (markdown-table
                          (map #(-> %
                                    (select-keys [:title :name :location :remote :url])
                                    translate-values
                                    (set/rename-keys {:title    "Vaga"
                                                      :name     "Empresa"
                                                      :location "Local"
                                                      :remote   "Remoto?"
                                                      :url      "Onde aplicar"}))
                               positions))

          :sem-vagas    (markdown-table (map transform-company unavailable))
          :uso-eventual (markdown-table (map transform-company eventual-use))}
         (selmer/render (slurp "README.md.template"))
         (spit "README.md"))))