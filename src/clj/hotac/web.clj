(ns hotac.web
  (:require [clojure.java.io :as io]
            [compojure.core :refer [defroutes GET POST context]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [response resource-response content-type redirect status]]
            (ring.middleware
               [defaults :refer :all]
               [session :refer [wrap-session]]
               [params :refer [wrap-params]]
               [keyword-params :refer [wrap-keyword-params]]
               [anti-forgery :refer [wrap-anti-forgery]])
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter sente-web-server-adapter]]
            [hotac.database :as db]))

;;;; Sente channel-socket            
(declare channel-socket)

(defn start-websocket []
  (defonce channel-socket
    (sente/make-channel-socket! (get-sch-adapter) )))
    
(defn get-upgrade-data []
  (slurp (io/resource "data/upgrades.json")))
(defn get-ship-data []
  (slurp (io/resource "data/ships.json")))
      
      ;(content-type (response (slurp (io/resource "data/wh40k_cards.min.json"))) "application/json"))
                         
(defroutes app-routes
  ;sente
  (GET  "/chsk"    req ((:ajax-get-or-ws-handshake-fn channel-socket) req))
  (POST "/chsk"    req ((:ajax-post-fn channel-socket) req))
  ;routes
  (GET "/" [] (slurp (io/resource "public/index.html")))
  (GET "/upgrades" []
    (content-type (response (get-upgrade-data)) "application/json"))
  (resources "/"))
  
(def app 
  (-> app-routes
    (wrap-keyword-params)
    (wrap-params)
    (wrap-session)
    (wrap-anti-forgery)))
    
    
;; multi to handle Sente 'events'
(defmulti event :id)

;; default for no other matching handler
(defmethod event :default [{:as ev-msg :keys [event]}]
  (println "Unhandled event: " event))

(defmethod event :chsk/ws-ping      [_])

(defmethod event :hotac/upgrades [{:as ev-msg :keys [event ?data ?reply-fn]}]
  (when ?reply-fn
    (?reply-fn (get-upgrade-data))))
(defmethod event :hotac/ships [{:as ev-msg :keys [event ?data ?reply-fn]}]
  (when ?reply-fn
    (?reply-fn (get-ship-data))))
    
(defmethod event :hotac/save-squad [{:as ev-msg :keys [event ?data ?reply-fn]}]
  (let [deck-id ((keyword "last_insert_rowid()") (-> (db/save-data (:squad ?data) (:id ?data)) first))]
    (println ?data)
    (println deck-id " created")
    (when ?reply-fn
      (?reply-fn (if deck-id deck-id (:id ?data))))))
    
;;;; Sente event router ('event' loop)
(defn start-router []
  (defonce router
    (sente/start-chsk-router! (:ch-recv channel-socket) event)))

    
;;;; Initalisation
    
(start-websocket)
(start-router)  