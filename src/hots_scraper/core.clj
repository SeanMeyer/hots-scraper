(ns hots-scraper.core
  (:require [hickory.core :refer :all])
  (:require [hickory.select :as s])
  (:require [clj-http.client :as client])
  (:require [clojure.string :as string])
  (:require [hots-scraper.headless :refer :all])
  (:require [korma.db :as db])
  (:require [korma.core :as korma])
  (:gen-class))


(defn table-body
  "Takes plain html for entire page, returns hickory tree for the hero data table"
  [site-html]
  (->> (parse site-html)
       (as-hickory)
       (s/select (s/child
                   (s/and
                     (s/tag :table)
                     (s/class "rgMasterTable"))
                   (s/tag :tbody)))
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
  [site-html]
  (let [table (table-body site-html)]
    (zipmap
      (map keyword (hero-names table))
      (map #(hash-map :winrate %1 :played %2)
            (hero-win-rates table)
            (hero-games-played table)))))

(defn add-league-to-map
  "Takes a map and adds data for league to that map, using league as keyword"
  [map league]
  (toggle-leagues [league])
  (let [new-map
        (assoc map league (hero-map (get-page-html)))]
    (toggle-leagues [league])
    new-map))


(defn all-data-map
  "Must already be on stats page, returns map with data for all leagues"
  []
  (reduce
    add-league-to-map
    {:all (hero-map (get-page-html))}
    [:master :diamond :platinum :gold :silver :bronze]))

(defn get-all-data
  []
  (open-hero-stats-page)
  (let [all-data (all-data-map)]
    (close-browser)
    all-data))


(defn -main []
  (-> (client/get "https://www.hotslogs.com/Sitewide/HeroAndMapStatistics")
      :body
      hero-map))

(db/defdb db (db/postgres {:db "hots-data"
                                       :user "postgres"
                                       :password "h163ry"
                                       :host "192.168.0.6"
                                       :port "5432"}))
(korma/defentity test)
(korma/select test)
                                       







