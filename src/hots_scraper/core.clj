(ns hots-scraper.core
  (:require [hickory.core :refer :all])
  (:require [hickory.select :as s])
  (:require [clj-http.client :as client])
  (:require [clojure.string :as string])
  (:gen-class))


(def site-tree (-> (client/get "https://www.hotslogs.com/Sitewide/HeroAndMapStatistics")
                   :body
                   parse
                   as-hickory))


(def table-body (-> (s/select (s/child
                                (s/and
                                  (s/tag :table)
                                  (s/class "rgMasterTable"))
                                (s/tag :tbody))
                      site-tree)
                    first))


(defn hero-td-contents
  "Return contents of the nth td per hero"
  [n]
  (map #(first (:content %))
    (s/select (s/child
                (s/tag :tr)
                (s/nth-of-type n :td))
      table-body)))


(def hero-names (map 
                  #(get-in % [:attrs :title])
                  (hero-td-contents 2)))


(def hero-win-rates (map #(-> (string/replace % " %" "")
                              Double/parseDouble)
                      (hero-td-contents 6)))


(def hero-games-played (map #(-> (string/replace % "," "")
                                 Integer/parseInt)
                         (hero-td-contents 3)))


(def hero-map (zipmap 
                (map keyword hero-names)
                (map #(hash-map :winrate %1 :played %2) 
                      hero-win-rates hero-games-played)))
                

(defn -main []
  (println hero-map))
