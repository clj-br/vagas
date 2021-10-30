(ns clojure-empregos-brasil.boards
  (:require [net.cgrand.enlive-html :as html]
            [clojure-empregos-brasil.scrap :as scrap]))

(def gupy
  {:title      [:.title html/text-node]
   :url        #(-> % (html/select [:a.job-list__item]) first :attrs :href)
   :location   (scrap/attr :data-workplace)
   :department (scrap/attr :data-department)
   :remote     (comp (partial = "true") (scrap/attr :data-remote))
   :type       (scrap/attr :data-type)})

(def greenhouse
  {:title      [:a html/text-node]
   :url        #(-> % (html/select [:a]) first :attrs :href)
   :location   [:.location html/text-node]
   :department (scrap/attr :department_id)
   :office_id  (scrap/attr :office_id)
   :remote     false})

(def breezy
  {:title      [:h2 html/text-node]
   :url        #(-> % (html/select [:a]) first :attrs :href)
   :location   [:ul.meta :li.location html/text-node]
   :department [:ul.meta :li.department html/text-node]
   :type       [:ul.meta :li.type html/text-node]})
