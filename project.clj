(defproject hots-scraper "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [hickory "0.7.1"]
                 [clj-http "3.6.0"]
                 [limo "0.1.8"]
                 [korma "0.4.3"]
                 [postgresql/postgresql "9.3-1102.jdbc41"]
                 [clj-time "0.13.0"]]  
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :aot [hots-scraper.core]
  :main hots-scraper.core)
