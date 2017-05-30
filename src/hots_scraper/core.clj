(ns hots-scraper.core
  (:require [hickory.core :refer :all])
  (:require [hickory.select :as s])
  (:require [clj-http.client :as client])
  (:require [clojure.string :as string])
  (:require [hots-scraper.headless :refer :all])
  (:require [korma.db :as db])
  (:require [korma.core :as korma])
  (:require [clj-time.core :as t])
  (:require [clj-time.coerce :as t2])
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
(empty? (korma/select test (korma/where {:id 7})))
(korma/insert test (korma/values {:text "From Clojure"}))

(defn insert-data [data]
  (defn hero-id [hero]
    (korma/defentity heroes)
    (defn getheroe
      (korma/select heroes (korma/where {:hero-name-key hero})))
    (defn puthero
      (korma/insert heroes (korma/values {:hero-name-key hero,
                                          :hero-name-display hero})))
    (let [sqlret (getheroe)]
      (if (empty? sqlret)
        ((puthero) :id)
        ((first sqlret) :id))))
  (defn league-id [league]
    (korma/defentity leagues)
    (defn getleague
      (korma/select leagues (korma/where {:league-name league})))
    (defn putleague
      (korma/insert heroes (korma/values {:league-name league})))
    (let [sqlret (getleague)]
      (if (empty? sqlret)
        ((putleague) :id)
        ((first sqlret) :id))))
  (defn sql-now[]
    (t2/to-sql-time (t/now)))
  (doseq [keyval data]
    (korma/defentity global-hero-data)
    (let [leagueid (league-id (key keyval))]
      (doseq [kv (val keyval)]
        (let [heroid (hero-id (key kv))]
          [v (val kv)]
          (korma/insert global-hero-data (korma/values {:league-id leagueid}
                                                      , :hero-id heroid
                                                      , :win-percent (v :winrate)
                                                      , :win-delta (v :??)
                                                      , :popularity (v :??)
                                                      , :games-played (v :played)
                                                      , :games-banned (v :??)
                                                      , :date (sql-now))))))))

(def sample-return
  {:all {:Ragnaros {:played 11031, :winrate 53.3},
         :Jaina {:played 14337, :winrate 50.5},
         :Diablo {:played 18944, :winrate 48.1},
         :Murky {:played 2869, :winrate 50.6},
         :Rehgar {:played 15697, :winrate 51.3},
         :Anub'arak {:played 26333, :winrate 55.1},
         :Zeratul {:played 6313, :winrate 46.5},
         :Nova {:played 6429, :winrate 48.3},
         :Abathur {:played 4368, :winrate 43.1},
         :Lúcio {:played 13142, :winrate 47.1},
         :Raynor {:played 7590, :winrate 47.8},
         :Li-Ming {:played 23198, :winrate 46.4},
         :D.Va {:played 13267, :winrate 53.2},
         :Cho {:played 102, :winrate 37.3},
         :Tyrande {:played 9166, :winrate 45.5},
         :Samuro {:played 7864, :winrate 55.1},
         :Sgt.Hammer {:played 3381, :winrate 51.9},
         :Gall {:played 100, :winrate 37.0},
         :Tyrael {:played 6610, :winrate 49.5},
         :Zarya {:played 8657, :winrate 48.5},
         :Zul'jin {:played 5447, :winrate 51.6},
         :Muradin {:played 17158, :winrate 44.5},
         :Lt.Morales {:played 8490, :winrate 49.5},
         :Valla {:played 29401, :winrate 50.6},
         :Chromie {:played 7318, :winrate 47.9},
         :Arthas {:played 22952, :winrate 50.4},
         :Varian {:played 28085, :winrate 49.5},
         :Tychus {:played 8310, :winrate 47.9},
         :Uther {:played 20424, :winrate 52.6},
         :Thrall {:played 5460, :winrate 45.5},
         :Kerrigan {:played 5054, :winrate 50.8},
         :Malfurion {:played 34400, :winrate 51.4},
         :Greymane {:played 19049, :winrate 51.0},
         :Tracer {:played 4931, :winrate 49.7},
         :Gul'dan {:played 18183, :winrate 48.5},
         :Azmodan {:played 13416, :winrate 53.4},
         :Tassadar {:played 2907, :winrate 40.5},
         :Stitches {:played 10623, :winrate 48.2},
         :Sonya {:played 21674, :winrate 53.1},
         :LiLi {:played 18446, :winrate 49.6},
         :TheLostVikings {:played 866, :winrate 48.4},
         :Artanis {:played 21660, :winrate 52.5},
         :Lunara {:played 8502, :winrate 49.0},
         :Chen {:played 3060, :winrate 48.1},
         :Sylvanas {:played 18333, :winrate 50.4},
         :Brightwing {:played 14774, :winrate 52.3},
         :Zagara {:played 7714, :winrate 51.8},
         :Alarak {:played 12284, :winrate 46.0},
         :Gazlowe {:played 3952, :winrate 52.9},
         :Johanna {:played 15997, :winrate 49.5},
         :Rexxar {:played 3153, :winrate 53.2},
         :Valeera {:played 6003, :winrate 45.6},
         :Medivh {:played 1622, :winrate 38.8},
         :E.T.C. {:played 8450, :winrate 45.2},
         :Dehaka {:played 8244, :winrate 46.4},
         :Illidan {:played 6088, :winrate 48.5},
         :Nazeebo {:played 25145, :winrate 53.1},
         :Cassia {:played 5880, :winrate 48.8},
         :Xul {:played 3154, :winrate 48.4},
         :Kael'thas {:played 26340, :winrate 51.2},
         :TheButcher {:played 7745, :winrate 52.7},
         :Kharazim {:played 11808, :winrate 50.2},
         :Auriel {:played 16460, :winrate 47.8},
         :Genji {:played 14586, :winrate 48.3},
         :Falstad {:played 14884, :winrate 48.4},
         :Probius {:played 3377, :winrate 55.5},
         :Leoric {:played 9880, :winrate 49.1}},
   :master {:Ragnaros {:played 1018, :winrate 59.6},
            :Jaina {:played 622, :winrate 51.6},
            :Diablo {:played 1472, :winrate 50.5},
            :Murky {:played 131, :winrate 48.9},
            :Rehgar {:played 1990, :winrate 54.7},
            :Anub'arak {:played 2807, :winrate 57.2},
            :Zeratul {:played 802, :winrate 47.3},
            :Nova {:played 322, :winrate 53.1},
            :Abathur {:played 575, :winrate 50.8},
            :Lúcio {:played 972, :winrate 48.4},
            :Raynor {:played 249, :winrate 47.4},
            :Li-Ming {:played 1623, :winrate 50.6},
            :D.Va {:played 1079, :winrate 51.7},
            :Cho {:played 35, :winrate 34.3},
            :Tyrande {:played 1033, :winrate 50.5},
            :Samuro {:played 219, :winrate 54.8},
            :Sgt.Hammer {:played 222, :winrate 51.8},
            :Gall {:played 35, :winrate 34.3},
            :Tyrael {:played 823, :winrate 52.4},
            :Zarya {:played 1102, :winrate 54.4},
            :Zul'jin {:played 317, :winrate 56.8},
            :Muradin {:played 1568, :winrate 48.3},
            :Lt.Morales {:played 425, :winrate 54.4},
            :Valla {:played 2720, :winrate 54.5},
            :Chromie {:played 567, :winrate 52.7},
            :Arthas {:played 2396, :winrate 53.0},
            :Varian {:played 1936, :winrate 49.6},
            :Tychus {:played 774, :winrate 51.4},
            :Uther {:played 2384, :winrate 55.8},
            :Thrall {:played 293, :winrate 52.6},
            :Kerrigan {:played 311, :winrate 54.3},
            :Malfurion {:played 3613, :winrate 51.4},
            :Greymane {:played 3378, :winrate 52.1},
            :Tracer {:played 183, :winrate 50.8},
            :Gul'dan {:played 1671, :winrate 53.1},
            :Azmodan {:played 1231, :winrate 52.9},
            :Tassadar {:played 135, :winrate 54.1},
            :Stitches {:played 1258, :winrate 52.3},
            :Sonya {:played 2240, :winrate 58.8},
            :LiLi {:played 741, :winrate 52.4},
            :TheLostVikings {:played 135, :winrate 44.4},
            :Artanis {:played 1462, :winrate 54.0},
            :Lunara {:played 742, :winrate 50.1},
            :Chen {:played 342, :winrate 48.8},
            :Sylvanas {:played 1168, :winrate 52.4},
            :Brightwing {:played 977, :winrate 52.1},
            :Zagara {:played 811, :winrate 58.8},
            :Alarak {:played 1990, :winrate 46.8},
            :Gazlowe {:played 121, :winrate 47.1},
            :Johanna {:played 1257, :winrate 52.4},
            :Rexxar {:played 220, :winrate 56.4},
            :Valeera {:played 205, :winrate 49.3},
            :Medivh {:played 263, :winrate 45.2},
            :E.T.C. {:played 677, :winrate 48.2},
            :Dehaka {:played 697, :winrate 48.5},
            :Illidan {:played 448, :winrate 52.0},
            :Nazeebo {:played 1704, :winrate 55.0},
            :Cassia {:played 776, :winrate 53.2},
            :Xul {:played 195, :winrate 57.4},
            :Kael'thas {:played 1684, :winrate 50.7},
            :TheButcher {:played 209, :winrate 59.8},
            :Kharazim {:played 1199, :winrate 50.2},
            :Auriel {:played 1705, :winrate 51.8},
            :Genji {:played 2213, :winrate 52.8},
            :Falstad {:played 1536, :winrate 51.9},
            :Probius {:played 321, :winrate 57.9},
            :Leoric {:played 1063, :winrate 51.2}},
   :diamond {:Ragnaros {:played 3740, :winrate 54.6},
             :Jaina {:played 3689, :winrate 54.3},
             :Diablo {:played 5815, :winrate 49.9},
             :Murky {:played 817, :winrate 51.5},
             :Rehgar {:played 5185, :winrate 53.8},
             :Anub'arak {:played 9280, :winrate 57.3},
             :Zeratul {:played 1792, :winrate 49.1},
             :Nova {:played 1703, :winrate 51.9},
             :Abathur {:played 1459, :winrate 45.7},
             :Lúcio {:played 3863, :winrate 48.6},
             :Raynor {:played 1808, :winrate 50.3},
             :Li-Ming {:played 6618, :winrate 48.1},
             :D.Va {:played 4161, :winrate 54.8},
             :Cho {:played 28, :winrate 35.7},
             :Tyrande {:played 3271, :winrate 48.5},
             :Samuro {:played 1646, :winrate 54.7},
             :Sgt.Hammer {:played 751, :winrate 51.4},
             :Gall {:played 27, :winrate 40.7},
             :Tyrael {:played 1961, :winrate 52.4},
             :Zarya {:played 3316, :winrate 49.8},
             :Zul'jin {:played 1534, :winrate 52.1},
             :Muradin {:played 4952, :winrate 45.3},
             :Lt.Morales {:played 2106, :winrate 52.8},
             :Valla {:played 9582, :winrate 52.1},
             :Chromie {:played 1841, :winrate 50.5},
             :Arthas {:played 8084, :winrate 52.2},
             :Varian {:played 8351, :winrate 49.7},
             :Tychus {:played 2598, :winrate 48.7},
             :Uther {:played 7441, :winrate 54.5},
             :Thrall {:played 1417, :winrate 50.0},
             :Kerrigan {:played 1335, :winrate 54.6},
             :Malfurion {:played 11149, :winrate 52.8},
             :Greymane {:played 7578, :winrate 52.3},
             :Tracer {:played 1073, :winrate 52.9},
             :Gul'dan {:played 6524, :winrate 49.6},
             :Azmodan {:played 4794, :winrate 52.7},
             :Tassadar {:played 732, :winrate 44.5},
             :Stitches {:played 3305, :winrate 50.8},
             :Sonya {:played 7545, :winrate 55.1},
             :LiLi {:played 4520, :winrate 50.9},
             :TheLostVikings {:played 279, :winrate 51.6},
             :Artanis {:played 6488, :winrate 53.6},
             :Lunara {:played 2577, :winrate 51.0},
             :Chen {:played 1057, :winrate 53.0},
             :Sylvanas {:played 5306, :winrate 52.9},
             :Brightwing {:played 4837, :winrate 53.7},
             :Zagara {:played 2512, :winrate 54.7},
             :Alarak {:played 5016, :winrate 47.3},
             :Gazlowe {:played 859, :winrate 51.2},
             :Johanna {:played 4916, :winrate 52.1},
             :Rexxar {:played 1015, :winrate 56.0},
             :Valeera {:played 1224, :winrate 47.2},
             :Medivh {:played 515, :winrate 42.1},
             :E.T.C. {:played 2461, :winrate 48.5},
             :Dehaka {:played 2724, :winrate 46.8},
             :Illidan {:played 1521, :winrate 54.0},
             :Nazeebo {:played 7949, :winrate 54.8},
             :Cassia {:played 1847, :winrate 49.8},
             :Xul {:played 902, :winrate 51.4},
             :Kael'thas {:played 8678, :winrate 52.5},
             :TheButcher {:played 1205, :winrate 56.1},
             :Kharazim {:played 3557, :winrate 51.6},
             :Auriel {:played 5534, :winrate 50.0},
             :Genji {:played 4798, :winrate 49.1},
             :Falstad {:played 5273, :winrate 50.7},
             :Probius {:played 1110, :winrate 58.6},
             :Leoric {:played 3626, :winrate 50.4}},
   :platinum {:Ragnaros {:played 2474, :winrate 53.1},
              :Jaina {:played 3350, :winrate 52.4},
              :Diablo {:played 4062, :winrate 49.8},
              :Murky {:played 651, :winrate 47.0},
              :Rehgar {:played 3347, :winrate 50.4},
              :Anub'arak {:played 5787, :winrate 55.1},
              :Zeratul {:played 1176, :winrate 46.7},
              :Nova {:played 1369, :winrate 52.2},
              :Abathur {:played 824, :winrate 40.9},
              :Lúcio {:played 2954, :winrate 46.9},
              :Raynor {:played 1792, :winrate 50.4},
              :Li-Ming {:played 5371, :winrate 46.7},
              :D.Va {:played 2989, :winrate 54.2},
              :Cho {:played 15, :winrate 46.7},
              :Tyrande {:played 1959, :winrate 44.2},
              :Samuro {:played 2007, :winrate 57.1},
              :Sgt.Hammer {:played 630, :winrate 53.3},
              :Gall {:played 14, :winrate 35.7},
              :Tyrael {:played 1239, :winrate 49.1},
              :Zarya {:played 1737, :winrate 47.0},
              :Zul'jin {:played 1185, :winrate 51.6},
              :Muradin {:played 3640, :winrate 45.1},
              :Lt.Morales {:played 1808, :winrate 48.7},
              :Valla {:played 6351, :winrate 51.8},
              :Chromie {:played 1577, :winrate 47.6},
              :Arthas {:played 4939, :winrate 50.3},
              :Varian {:played 6337, :winrate 50.9},
              :Tychus {:played 1911, :winrate 48.8},
              :Uther {:played 4415, :winrate 51.8},
              :Thrall {:played 1160, :winrate 45.9},
              :Kerrigan {:played 1101, :winrate 51.4},
              :Malfurion {:played 7297, :winrate 52.5},
              :Greymane {:played 3566, :winrate 51.4},
              :Tracer {:played 1002, :winrate 45.9},
              :Gul'dan {:played 4232, :winrate 48.6},
              :Azmodan {:played 3000, :winrate 55.1},
              :Tassadar {:played 670, :winrate 40.9},
              :Stitches {:played 2276, :winrate 47.5},
              :Sonya {:played 4656, :winrate 53.8},
              :LiLi {:played 4044, :winrate 51.5},
              :TheLostVikings {:played 172, :winrate 45.3},
              :Artanis {:played 4918, :winrate 53.3},
              :Lunara {:played 1800, :winrate 47.9},
              :Chen {:played 630, :winrate 50.8},
              :Sylvanas {:played 4093, :winrate 52.3},
              :Brightwing {:played 3404, :winrate 53.1},
              :Zagara {:played 1740, :winrate 51.9},
              :Alarak {:played 2421, :winrate 44.7},
              :Gazlowe {:played 1074, :winrate 55.8},
              :Johanna {:played 3721, :winrate 50.0},
              :Rexxar {:played 734, :winrate 54.0},
              :Valeera {:played 1392, :winrate 49.5},
              :Medivh {:played 326, :winrate 32.8},
              :E.T.C. {:played 1885, :winrate 45.3},
              :Dehaka {:played 1888, :winrate 46.6},
              :Illidan {:played 1267, :winrate 49.1},
              :Nazeebo {:played 5922, :winrate 54.2},
              :Cassia {:played 1249, :winrate 47.8},
              :Xul {:played 766, :winrate 47.3},
              :Kael'thas {:played 6584, :winrate 51.5},
              :TheButcher {:played 1558, :winrate 55.4},
              :Kharazim {:played 2517, :winrate 52.5},
              :Auriel {:played 3482, :winrate 47.6},
              :Genji {:played 2928, :winrate 47.5},
              :Falstad {:played 3415, :winrate 47.3},
              :Probius {:played 740, :winrate 55.0},
              :Leoric {:played 2119, :winrate 49.2}},
   :gold {:Ragnaros {:played 1236, :winrate 47.7},
          :Jaina {:played 1893, :winrate 50.0},
          :Diablo {:played 2187, :winrate 48.2},
          :Murky {:played 413, :winrate 53.8},
          :Rehgar {:played 1604, :winrate 51.6},
          :Anub'arak {:played 2733, :winrate 54.3},
          :Zeratul {:played 731, :winrate 45.4},
          :Nova {:played 780, :winrate 45.9},
          :Abathur {:played 440, :winrate 36.6},
          :Lúcio {:played 1649, :winrate 49.4},
          :Raynor {:played 1014, :winrate 48.0},
          :Li-Ming {:played 2888, :winrate 45.9},
          :D.Va {:played 1517, :winrate 54.2},
          :Cho {:played 10, :winrate 10.0},
          :Tyrande {:played 864, :winrate 41.1},
          :Samuro {:played 1109, :winrate 54.3},
          :Sgt.Hammer {:played 426, :winrate 54.9},
          :Gall {:played 11, :winrate 45.5},
          :Tyrael {:played 668, :winrate 45.8},
          :Zarya {:played 763, :winrate 44.4},
          :Zul'jin {:played 677, :winrate 52.6},
          :Muradin {:played 1960, :winrate 44.0},
          :Lt.Morales {:played 1075, :winrate 51.0},
          :Valla {:played 3274, :winrate 48.7},
          :Chromie {:played 963, :winrate 49.9},
          :Arthas {:played 2298, :winrate 49.7},
          :Varian {:played 3377, :winrate 48.3},
          :Tychus {:played 981, :winrate 49.3},
          :Uther {:played 2184, :winrate 50.8},
          :Thrall {:played 750, :winrate 44.7},
          :Kerrigan {:played 694, :winrate 50.1},
          :Malfurion {:played 3663, :winrate 52.2},
          :Greymane {:played 1437, :winrate 50.0},
          :Tracer {:played 825, :winrate 51.3},
          :Gul'dan {:played 2139, :winrate 47.5},
          :Azmodan {:played 1450, :winrate 54.6},
          :Tassadar {:played 374, :winrate 39.6},
          :Stitches {:played 1097, :winrate 47.0},
          :Sonya {:played 2217, :winrate 52.3},
          :LiLi {:played 2563, :winrate 50.5},
          :TheLostVikings {:played 105, :winrate 53.3},
          :Artanis {:played 2712, :winrate 53.1},
          :Lunara {:played 901, :winrate 47.8},
          :Chen {:played 280, :winrate 42.9},
          :Sylvanas {:played 2407, :winrate 48.0},
          :Brightwing {:played 1802, :winrate 51.6},
          :Zagara {:played 822, :winrate 48.7},
          :Alarak {:played 988, :winrate 45.7},
          :Gazlowe {:played 593, :winrate 52.8},
          :Johanna {:played 1955, :winrate 49.3},
          :Rexxar {:played 356, :winrate 51.7},
          :Valeera {:played 816, :winrate 44.6},
          :Medivh {:played 173, :winrate 40.5},
          :E.T.C. {:played 1026, :winrate 44.4},
          :Dehaka {:played 955, :winrate 46.4},
          :Illidan {:played 759, :winrate 47.6},
          :Nazeebo {:played 3012, :winrate 52.2},
          :Cassia {:played 643, :winrate 46.7},
          :Xul {:played 384, :winrate 45.8},
          :Kael'thas {:played 3239, :winrate 51.1},
          :TheButcher {:played 1185, :winrate 51.1},
          :Kharazim {:played 1254, :winrate 50.2},
          :Auriel {:played 1606, :winrate 46.6},
          :Genji {:played 1457, :winrate 48.2},
          :Falstad {:played 1602, :winrate 47.2},
          :Probius {:played 426, :winrate 53.3},
          :Leoric {:played 977, :winrate 48.7}},
   :silver {:Ragnaros {:played 903, :winrate 52.6},
            :Jaina {:played 1490, :winrate 48.9},
            :Diablo {:played 1503, :winrate 46.7},
            :Murky {:played 313, :winrate 54.0},
            :Rehgar {:played 1130, :winrate 46.1},
            :Anub'arak {:played 1915, :winrate 51.3},
            :Zeratul {:played 477, :winrate 44.4},
            :Nova {:played 641, :winrate 41.3},
            :Abathur {:played 357, :winrate 41.5},
            :Lúcio {:played 1095, :winrate 43.7},
            :Raynor {:played 812, :winrate 47.8},
            :Li-Ming {:played 2003, :winrate 43.9},
            :D.Va {:played 1256, :winrate 51.9},
            :Cho {:played 11, :winrate 45.5},
            :Tyrande {:played 609, :winrate 38.8},
            :Samuro {:played 856, :winrate 53.9},
            :Sgt.Hammer {:played 349, :winrate 53.3},
            :Gall {:played 5, :winrate 20.0},
            :Tyrael {:played 488, :winrate 49.4},
            :Zarya {:played 528, :winrate 45.8},
            :Zul'jin {:played 475, :winrate 50.1},
            :Muradin {:played 1360, :winrate 42.3},
            :Lt.Morales {:played 848, :winrate 48.7},
            :Valla {:played 2281, :winrate 48.6},
            :Chromie {:played 697, :winrate 44.8},
            :Arthas {:played 1538, :winrate 47.5},
            :Varian {:played 2500, :winrate 50.5},
            :Tychus {:played 692, :winrate 45.7},
            :Uther {:played 1224, :winrate 48.3},
            :Thrall {:played 529, :winrate 44.4},
            :Kerrigan {:played 531, :winrate 49.2},
            :Malfurion {:played 2570, :winrate 50.4},
            :Greymane {:played 950, :winrate 47.9},
            :Tracer {:played 546, :winrate 50.2},
            :Gul'dan {:played 1304, :winrate 43.7},
            :Azmodan {:played 953, :winrate 52.0},
            :Tassadar {:played 264, :winrate 43.2},
            :Stitches {:played 719, :winrate 45.2},
            :Sonya {:played 1538, :winrate 49.9},
            :LiLi {:played 1835, :winrate 48.9},
            :TheLostVikings {:played 65, :winrate 46.2},
            :Artanis {:played 1682, :winrate 50.4},
            :Lunara {:played 762, :winrate 47.8},
            :Chen {:played 216, :winrate 36.6},
            :Sylvanas {:played 1774, :winrate 48.3},
            :Brightwing {:played 1304, :winrate 51.6},
            :Zagara {:played 670, :winrate 47.6},
            :Alarak {:played 625, :winrate 41.4},
            :Gazlowe {:played 483, :winrate 53.2},
            :Johanna {:played 1467, :winrate 45.3},
            :Rexxar {:played 244, :winrate 51.2},
            :Valeera {:played 669, :winrate 41.3},
            :Medivh {:played 79, :winrate 46.8},
            :E.T.C. {:played 705, :winrate 41.0},
            :Dehaka {:played 693, :winrate 45.0},
            :Illidan {:played 544, :winrate 40.4},
            :Nazeebo {:played 2026, :winrate 50.2},
            :Cassia {:played 438, :winrate 50.2},
            :Xul {:played 293, :winrate 45.4},
            :Kael'thas {:played 2283, :winrate 51.3},
            :TheButcher {:played 1046, :winrate 52.7},
            :Kharazim {:played 905, :winrate 46.3},
            :Auriel {:played 1097, :winrate 42.5},
            :Genji {:played 839, :winrate 41.8},
            :Falstad {:played 1001, :winrate 45.4},
            :Probius {:played 273, :winrate 52.0},
            :Leoric {:played 668, :winrate 45.7}},
   :bronze {:Ragnaros {:played 1023, :winrate 49.5},
            :Jaina {:played 1844, :winrate 42.4},
            :Diablo {:played 2044, :winrate 40.9},
            :Murky {:played 336, :winrate 47.6},
            :Rehgar {:played 1356, :winrate 43.1},
            :Anub'arak {:played 2117, :winrate 45.3},
            :Zeratul {:played 713, :winrate 40.0},
            :Nova {:played 916, :winrate 39.1},
            :Abathur {:played 378, :winrate 33.6},
            :Lúcio {:played 1498, :winrate 42.9},
            :Raynor {:played 1177, :winrate 43.1},
            :Li-Ming {:played 2669, :winrate 39.7},
            :D.Va {:played 1551, :winrate 48.2},
            :Cho {:played 1, :winrate 100.0},
            :Tyrande {:played 667, :winrate 36.6},
            :Samuro {:played 1332, :winrate 54.0},
            :Sgt.Hammer {:played 698, :winrate 49.4},
            :Gall {:played 3, :winrate 66.7},
            :Tyrael {:played 613, :winrate 40.6},
            :Zarya {:played 556, :winrate 41.2},
            :Zul'jin {:played 675, :winrate 49.3},
            :Muradin {:played 1955, :winrate 40.9},
            :Lt.Morales {:played 1356, :winrate 42.9},
            :Valla {:played 2703, :winrate 42.4},
            :Chromie {:played 1007, :winrate 41.6},
            :Arthas {:played 1866, :winrate 42.2},
            :Varian {:played 3351, :winrate 46.6},
            :Tychus {:played 803, :winrate 40.0},
            :Uther {:played 1477, :winrate 46.3},
            :Thrall {:played 752, :winrate 35.6},
            :Kerrigan {:played 684, :winrate 45.5},
            :Malfurion {:played 3299, :winrate 44.6},
            :Greymane {:played 1043, :winrate 43.3},
            :Tracer {:played 637, :winrate 47.3},
            :Gul'dan {:played 1344, :winrate 43.5},
            :Azmodan {:played 1208, :winrate 51.9},
            :Tassadar {:played 361, :winrate 31.3},
            :Stitches {:played 1024, :winrate 42.9},
            :Sonya {:played 1772, :winrate 43.3},
            :LiLi {:played 2678, :winrate 44.2},
            :TheLostVikings {:played 72, :winrate 48.6},
            :Artanis {:played 2216, :winrate 48.1},
            :Lunara {:played 1017, :winrate 45.4},
            :Chen {:played 276, :winrate 39.5},
            :Sylvanas {:played 2274, :winrate 45.6},
            :Brightwing {:played 1561, :winrate 49.1},
            :Zagara {:played 719, :winrate 43.7},
            :Alarak {:played 649, :winrate 38.2},
            :Gazlowe {:played 656, :winrate 52.6},
            :Johanna {:played 1647, :winrate 43.5},
            :Rexxar {:played 285, :winrate 43.9},
            :Valeera {:played 920, :winrate 38.8},
            :Medivh {:played 141, :winrate 36.9},
            :E.T.C. {:played 892, :winrate 36.9},
            :Dehaka {:played 777, :winrate 44.0},
            :Illidan {:played 831, :winrate 42.4},
            :Nazeebo {:played 2572, :winrate 48.0},
            :Cassia {:played 609, :winrate 42.0},
            :Xul {:played 451, :winrate 44.6},
            :Kael'thas {:played 2457, :winrate 47.0},
            :TheButcher {:played 1598, :winrate 48.6},
            :Kharazim {:played 1252, :winrate 43.0},
            :Auriel {:played 1315, :winrate 38.9},
            :Genji {:played 1032, :winrate 38.0},
            :Falstad {:played 1228, :winrate 41.2},
            :Probius {:played 319, :winrate 48.6},
            :Leoric {:played 814, :winrate 41.9}}})


