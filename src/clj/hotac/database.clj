(ns hotac.database
  (:require
    [clojure.java.jdbc :refer [db-do-commands create-table-ddl insert!] :as jdbc]))
           

(def db (or (System/getenv "DATABASE_URL")
            {:classname   "org.sqlite.JDBC"
             :subprotocol "sqlite"
             :subname     "db/hotacdb.sqlite3"}))
   
(defn- create-db []
   (try (db-do-commands db
            (create-table-ddl :squads
               [[:uid  :integer :primary :key :AUTOINCREMENT]
                [:data :text]]))
        (jdbc/insert! db :sqlite_sequence {:name "squads" :seq 1000})
        (catch Exception e ())))

(defn update-or-insert!
  "Updates columns or inserts a new row in the specified table"
  [db table row where-clause]
  (jdbc/with-db-transaction [t-con db]
    (let [result (jdbc/update! t-con table row where-clause)]
      (if (zero? (first result))
        (jdbc/insert! t-con table row)
        result))))        
           
(defn save-data [squad uid]
   (create-db)
   (update-or-insert! db :squads {:data squad} ["uid = ?" uid]))
   
(defn get-squads []
  (try (jdbc/query db ["SELECT * FROM squads"])
      (catch Exception e ())))