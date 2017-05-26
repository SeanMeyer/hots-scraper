(defproject hots-scraper "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [hickory "0.7.1"]
                 [clj-http "3.6.0"]]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :aot [hots-scraper.core]
  :main hots-scraper.core)
