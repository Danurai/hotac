(ns hotac.web
  (:require [chord.http-kit :refer [with-channel]]
           [clojure.java.io :as io]
           [compojure.core :refer [defroutes GET]]
           [compojure.route :refer [resources]]
           [clojure.core.async :refer [>! <! go go-loop]]
           [clojure.java.jdbc :as sql]))

(def db {:classname "org.sqlite.JDBC", :subprotocol "sqlite", :subname "test.db"})

(defn- save-data [squad]
 ;;(prn (sql/query db "select 3*5 as result"))
   )
           
(defn ws-handler [req]
   (with-channel req ws-ch
      (go
         (>! ws-ch {:squadname nil})
         (loop []
            (when-let [{:keys [message]} (<! ws-ch)]
               (prn message)
               (save-data message)
               (recur))))))

           
(defroutes app
  (GET "/ws" [] ws-handler)
  (GET "/" [] (slurp (io/resource "public/index.html")))
  (resources "/"))