(ns clojure-empregos-brasil.breezy
  (:require [net.cgrand.enlive-html :as html]
            [clj-http.client :as http])
  (:import  [org.mozilla.javascript CompilerEnvirons Context Parser Token]
            [org.mozilla.javascript.ast NodeVisitor]))

(def ^:private i18n-map (atom nil))

(defn- parse-js
  [source uri]
  (let [env (new CompilerEnvirons)]
    (.setLanguageVersion env Context/VERSION_ES6)

    (-> (Parser. env)
        (.parse source uri 0))))

(defn- translation-var?
  [node]
  (let [node (.getTarget node)]
    (and (= (.getType node) Token/NAME)
         (= (.getIdentifier node) "TRANSLATIONS"))))

(defn- make-visitor
  [target]
  (reify NodeVisitor
    (visit [this node]
      (if (= (.getType node) Token/VAR)
        (do
          (when-let [node (first (filter translation-var? (.getVariables node)))]
            (reset! target (.getInitializer node)))
          false)
        true))))

(defn- find-translation-node
  [ast]
  (let [translation-node (atom nil)
        visitor (make-visitor translation-node)]
    (.visitAll ast visitor)
    @translation-node))

(defn- node->str
  [node]
  (let [type (.getType node)]
    (cond
      (= type Token/NAME) (.getIdentifier node)
      (= type Token/STRING) (.getValue node)
      (= type Token/NUMBER) (.getValue node)
      :else nil)))

(def object-literal->hashmap)

(defn- element->pair
  [element]
  (let [left (.getLeft element)
        right (.getRight element)]
    [(node->str left) (if (= (.getType right) Token/OBJECTLIT)
                        (object-literal->hashmap right)
                        (node->str right))]))

(defn- object-literal->hashmap
  [literal]
  (if (not= (.getType literal) Token/OBJECTLIT)
    nil
    (into {} (map element->pair (.getElements literal)))))

(defn- translate-script-src
  [page]
  (-> page (html/select [:#translateScript]) first :attrs :src))

(defn- fetch-script
  [url]
  (-> url http/get :body))

(defn load-i18n
  [page]
  (let [translate-script (translate-script-src page)]
    (reset! i18n-map
            (-> translate-script
                fetch-script
                (parse-js translate-script)
                find-translation-node
                object-literal->hashmap
                (get "pt-br")))))
      

(defn i18n
  [key]
  (get @i18n-map key key))
  
