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
  "Given a vector of league keywords, load those leagues on page
   Returns page HTML after toggling"
  [leagues]
  (api/click "#ComboBoxLeague_Input")
  (doseq [key leagues]
    (api/click (format "#ComboBoxLeague_DropDown li:nth-of-type(%d)"
                 (key league))))
  (api/click "#body")
  (api/wait-until-clickable "#MainContent_divControlContainer")
  (Thread/sleep 1000)
  (api/attribute "html" "innerHTML"))


(defn close-browser
  "Closes open chrome instance if one exists"
  []
  (api/quit))


(defn open-hero-stats-page
  "Opens browser and goes to the hero and map statistics page"
  []
  (api/set-driver! (driver/create-chrome))
  (api/to "https://www.hotslogs.com/Sitewide/HeroAndMapStatistics"))


(defn selenium-test
  []
  (open-hero-stats-page)
  (let [result (toggle-leagues [:master])]
    (close-browser)
    result))