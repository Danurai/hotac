(ns hotac.communication
  (:require 
    [hotac.model :as model]
    [taoensso.sente :as sente]))

(defonce channel-socket
  (sente/make-channel-socket! "/chsk" {:type :auto :ws-kalive-ms 20000}))
(defonce chsk (:chsk channel-socket))
(defonce ch-chsk (:ch-recv channel-socket))
(defonce chsk-send! (:send-fn channel-socket))
(defonce chsk-state (:state channel-socket))

;;;; Sente send functions

(defn load-upgrades! []
  (chsk-send! [:hotac/upgrades] 
              5000
              (fn [cb-reply] (reset! model/upgrades (js->clj (.parse js/JSON cb-reply) :keywordize-keys true)))))
(defn load-ships! []
  (chsk-send! [:hotac/ships] 
              5000
              (fn [cb-reply] (reset! model/ships (js->clj (.parse js/JSON cb-reply) :keywordize-keys true)))))
               
(defn save-squad! []
  (chsk-send! [:hotac/save-squad {:squad (.stringify js/JSON (clj->js @model/app-data)) :id (:id @model/app-data)}]
              5000
              (fn [cb-reply] (swap! model/app-data assoc :id cb-reply :saved true)(prn cb-reply))
              ))
              
;;;; Sente event handlers

(defmulti event-msg-handler :id)
  
(defmethod event-msg-handler :default [_]
)

(defmethod event-msg-handler :chsk/handshake [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (println "Handshake:" ?data)
    (load-upgrades!)
    (load-ships!)
    ))
    
;;;; Sente event router ('event-msg-handler' loop)
(defonce router
  (sente/start-client-chsk-router! ch-chsk event-msg-handler))
