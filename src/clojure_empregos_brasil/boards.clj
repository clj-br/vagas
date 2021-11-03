(ns clojure-empregos-brasil.boards
  (:require [net.cgrand.enlive-html :as html]
            [clojure-empregos-brasil.scrap :as scrap]
            [clojure-empregos-brasil.breezy :as breezy]))

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
   :location   #(-> %
                    (html/select [:ul.meta :li.location html/text-node])
                    first
                    breezy/i18n)
   :department [:ul.meta :li.department html/text-node]
   :type       [:ul.meta :li.type html/text-node]
   :remote     #(-> %
                    (html/select [:ul.meta :li.location html/text-node])
                    (->> first (contains? #{"%LABEL_POSITION_TYPE_REMOTE%"
                                            "%LABEL_POSITION_TYPE_Worldwide%"
                                            "%LABEL_POSITION_TYPE_WORLDWIDE%"})))})
