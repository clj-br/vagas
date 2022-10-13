(ns clojure-empregos-brasil.companies
  (:require [clojure.string :as string]
            [clojure-empregos-brasil.boards :as boards]
            [clojure-empregos-brasil.breezy :as breezy]))

(def nubank
  {:name      "Nubank"
   :page      "https://boards.greenhouse.io/nubank"
   :path      [:div.opening]
   :scrap     boards/greenhouse
   :engineer? (comp (partial = "60350") :department)
   :brazil?   (comp (partial = "58102") :office_id)
   :clojure?  boolean})

(def paygo
  {:name      "PayGo"
   :page      "https://paygo.gupy.io/"
   :path      [:.job-list :tr]
   :scrap     (assoc boards/gupy :remote boolean)         ;; all PayGo positions are remote
   :engineer? #(= (:department %) "Tecnologia")
   :brazil?   boolean
   :clojure?  #(string/includes?
                 (-> % :title string/lower-case)
                 "clojure")})

(def embraer
  {:name      "Embraer"
   :page      "https://embraer.gupy.io/"
   :path      [:.job-list :tr]
   :scrap     boards/gupy
   :engineer? #(= (:department %) "Inovação")
   :brazil?   :remote
   :clojure?  #(string/includes?
                 (-> % :title string/lower-case)
                 "clojure")})

(def pipo-saude
  {:name      "Pipo Saúde"
   :page      "https://pipo-saude.breezy.hr/"
   :path      [:li.position]
   :scrap     boards/breezy
   :engineer? #(= (:department %) "Engenharia")
   :brazil?   boolean
   :clojure?  boolean
   :pre-html  #(do (breezy/load-i18n %) %)})

(def i80-seguros
  {:name      "180° Seguros"
   :page      "https://180-seguros.breezy.hr/"
   :path      [:li.position]
   :scrap     boards/breezy
   :engineer? #(= (:department %) "Tecnologia")
   :brazil?   boolean
   :clojure?  boolean})