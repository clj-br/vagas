(ns clojure-empregos-brasil.scraping
  (:require [clojure.string :as string]
            [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html]
            [selmer.parser :as selmer]))

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
   :scrap     (assoc gupy :remote boolean)                  ;; all PayGo positions are remote
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

(defn scrap-all
  "Scrap all companies filtering Clojure brazilian engineers and returning a full
   sequence."
  [& companies]
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

(defn partial-right
  "Takes a function f with partial arguments r-args and returns a function
   that apply those arguments to the right."
  [f & r-args]
  (fn [& args]
    (apply f (concat args r-args))))

(defn -main [& args]
  (let [positions (sort-by (juxt :name :title)
                           (concat (scrap-all nubank paygo embraer pipo-saude)
                                   (edn/read (java.io.PushbackReader. (io/reader "jobs.edn")))))
        companies (edn/read (java.io.PushbackReader. (io/reader "companies.edn")))

        {:keys [unavailable eventual-use]} (group-by :situation companies)]
    (spit "scraped-jobs.edn" (pr-str positions))

    (->> {:vagas        (markdown-table
                          (map #(-> %
                                    (select-keys [:title :name :location :remote :url])

                                    (update :remote {true "Sim" false "Não"})
                                    (update :title (partial-right string/replace #"\|" "-"))

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