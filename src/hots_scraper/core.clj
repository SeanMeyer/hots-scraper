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
  [n table-body]
  (map #(first (:content %))
    (s/select (s/child
                (s/tag :tr)
                (s/nth-of-type n :td))
      table-body)))


(defn hero-names
  "Return sequence of hero names, spaces stripped"
  [table-body]
  (map
    #(-> (get-in % [:attrs :title])
         (string/replace " " ""))
    (hero-td-contents 2 table-body)))


(defn hero-win-rates
  "Return sequence of win rates as doubles"
  [table-body]
  (map #(-> (string/replace % " %" "")
            Double/parseDouble)
    (hero-td-contents 6 table-body)))


(defn hero-games-played
  "Return sequence of games played as integers"
  [table-body]
  (map #(-> (string/replace % "," "")
            Integer/parseInt)
    (hero-td-contents 3 table-body)))


(defn hero-map
  "Return map of hero details"
  [table-body]
  (zipmap
    (map keyword (hero-names table-body))
    (map #(hash-map :winrate %1 :played %2)
          (hero-win-rates table-body)
          (hero-games-played table-body))))
                

(defn -main []
  (println (hero-map table-body)))
