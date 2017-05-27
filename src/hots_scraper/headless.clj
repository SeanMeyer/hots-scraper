(ns hots-scraper.headless
  (:require [limo.api :as api]
   :require [limo.driver :as driver]))


(def league {:master 1
             :diamond 2
             :platinum 3
             :gold 4
             :silver 5
             :bronze 6})


(defn toggle-leagues
  "Given a vector of league keywords, load those leagues on page"
  [leagues]
  (do
    (api/click "#ComboBoxLeague_Input")
    (doseq [key leagues]
      (api/click (format "#ComboBoxLeague_DropDown li:nth-of-type(%d)" 
                   (key league))))
    (api/click "#body")))


(defn selenium-test
  []
  (do
    (api/set-driver! (driver/create-chrome))
    (api/to "https://www.hotslogs.com/Sitewide/HeroAndMapStatistics")
    (toggle-leagues [:platinum :silver])))