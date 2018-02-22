(ns hotac.core
   (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! put! go]]
            [reagent.core :as r]
            [hotac.datastore :as ds]
            [hotac.upgrades :as ug]
            [hotac.controller :refer [assign-upgrade-slot]]))
   
(enable-console-print!)
(goog-define ws-uri "ws://localhost:9009/ws")

(def app-data (r/atom nil))
(def app-state (r/atom nil))


(def upgradedata (js->clj (.parse js/JSON ug/upgrade-json) :keywordize-keys true))

(defn- new-pilot [ship]
  {:xp (:startxp ship)
   :id (if (= 0 (->> @app-data :pilots count))
          1
          (->> @app-data :pilots (map :id) (apply max) inc))
   :ps 2
   :ship (dissoc ship :info)
   :upgradeslots (map-indexed (fn [idx x] {:id idx :slot x}) (conj (:slots ship) "Modification") )
   :upgrades []})
   
(defn- add-pilot [ship]
   (swap! app-data update :pilots conj (new-pilot ship)))         

(defn- new-squad []
   (reset! app-data {:squadname nil :pilots []}))
   
(defn- statline [stats]
   (let [statsymbols {:ps "x" :attack "%" :evade "^" :hull "&" :shield "*"}]
      (for [stat stats]
         ^{:key stat}[:span {:class (str "xwing-" (name (key stat)))}
            [:span {:class "xwing-font-b mr-1"} (val stat)]
            [:span {:class "xwing-symbols mr-1"} ((key stat) statsymbols)]])))
   
(defn- ship-button [ship]
   ^{:key ship}
   [:button.btn.btn-outline-secondary
      {:data-dismiss "modal"
       :on-click #(add-pilot ship)}
      [:span.h3
         [:span.h1.xwing-ships.mr-1 (:symbol ship)]
         [:span.xwing-font-b (:name ship)]
         [:span.badge.badge-secondary.ml-2 (statline (select-keys ship [:attack :evade :hull :shields]))]]
      [:p.font-italic.xwing-btn-wrap (:info ship)]])

(defn- ship-modal []
   [:div#select-ship.modal.fade {:aria-hidden "true" :role "dialog"}
      [:div.modal-dialog.modal-dialog-centered
         [:div.modal-content
            [:div.modal-header
               [:h5.modal-title "Select a starting ship"]
               [:button.close {:data-dismiss "modal"}
                  [:span "x"]]]
            [:div.modal-body
               (map #(ship-button %) ds/ships)]]]])
            
(defn- do-modal-action []
   (let [action (-> @app-data :confirm :action)]
      (case (:type action)
         :delete-pilot  (swap! app-data assoc :pilots (->> @app-data :pilots (remove #(= (:idx action) (:id %)))))
         :default)
      (swap! app-data dissoc :actions)))
   
(defn- confirm-modal []
   [:div#confirm-modal.modal.fade
      [:div.modal-dialog.modal-dialog-centered
         [:div.modal-content
            [:div.modal-header
               [:h5.modal-title "Please Confirm"]
               [:button.close {:data-dismiss "modal"}
                  [:span "x"]]]
            [:div.modal-body
               [:span.h6 (-> @app-data :confirm :message)]]
            [:div.modal-footer
               [:button.btn.btn-secondary {:data-dismiss "modal"} "Close"]
               [:button.btn.btn-warning {:on-click #(do-modal-action) :data-dismiss "modal"} "Affirmative"]]]]])
               
(defn- navbar [ws-ch]
   [:nav.navbar.navbar-dark.bg-dark.justify-content-between.mb-2
      [:span.navbar-brand.h1 "HotAC - Squad Builder"]
      [:span   
         [:button.btn.btn-danger.mr-2 {:on-click (fn [e] (put! ws-ch @app-data))} "Save Squad"]
         [:button.btn.btn-primary {:on-click new-squad} "New Squad"]]])
      
(defn- render-squad-header []
   [:div.input-group.mb-3
      [:div.input-group-prepend
         [:span.input-group-text "Squad Name"]]
      [:input.form-control 
         {:placeholder "Name your squad" 
          :value (:squadname @app-data)
          :on-change #(swap! app-data assoc :squadname (-> % .-target .-value) )}]
      [:div.input-group-append
         [:button.btn.btn-outline-primary 
            {:data-toggle "modal" :data-target "#select-ship"} "Add Pilot"]]])

(defn- setmodal-delete-pilot [pilot]
   (swap! app-data assoc :confirm {:message (str "Are you sure you want to permanently delete the " (:type pilot) " pilot " (if (some? (:callsign pilot)) (:callsign pilot) "with no name") "?")
                                :action {:type :delete-pilot :idx (:id pilot)}}))

(defn- update-pilot [id key value]
   (swap! app-data assoc :pilots (map #(if (= id (:id %)) (assoc % key value) % ) (:pilots @app-data) )))
   
(defn- assign-upgrade [pilotid upgradeid slotid]
   (swap! app-data assoc :pilots (assign-upgrade-slot pilotid upgradeid slotid (:pilots @app-data))))
 
(defn- input-group-select [pilotid upgrade options]
   ^{:key (:id upgrade)}[:div.input-group.mb-1
      [:div.input-group-prepend
         [:span.input-group-text.xwing-symbols (ds/slot-symbol (:slot upgrade))]]
      [:select.form-control {:value (:name (->> @app-data 
                                           :pilots 
                                           (filter #(= (:id %) pilotid)) 
                                           first 
                                           :upgrades 
                                           (filter #(= (:slotid %) (:id upgrade)))
                                           first)
                                       (str "No " (:slot upgrade) " selected"))
                          :on-change #(assign-upgrade 
                                       pilotid
                                       (.-id (aget (-> % .-target .-options) (-> % .-target .-selectedIndex)))
                                       (:id upgrade))}
         (for [opt (concat [{:name (str "No " (:slot upgrade) " selected") :default true :id -1}] options)]
            ^{:key opt}[:option {:hidden (:default opt) :disabled (:default opt) :id (:id opt)}
                        (:name opt) ])]
      [:div.input-group-append
         [:span.input-group-text.btn.btn-secondary 
            {:data-toggle "modal"
             :data-target "#upgrade-modal"
             :on-click (fn [e]
                          (reset! app-state {:upgradelist (->> upgradedata (filter #(= (:slot upgrade) (:slot %))))
                                           :pilotid pilotid
                                           :xp (->> @app-data :pilots (filter #(= pilotid (:id %))) first :xp)
                                           :slotid (:id upgrade)}))} "+"]]])
  


(defn- buy-upgrade []
   (let [pilotid (-> @app-state :pilotid)
        cost    (-> @app-state :selected :points)
        upgradeid (str (-> @app-state :selected :id) 
                     "-"
                     (->> @app-data :pilots (filter #(= pilotid (:id %))) first :upgrades (filter #(= (-> @app-state :selected :name) (:name %))) count))
        upgrade (-> @app-state :selected (select-keys [:name :slot :xws]) (assoc :slotid (:slotid @app-state) :id upgradeid) ) ]
        
      (swap! app-data assoc :pilots (map #(if (= pilotid (:id %)) 
                                          (update % :upgrades conj upgrade)
                                          %) (:pilots @app-data)))
      (swap! app-data assoc :pilots (map #(if (= pilotid (:id %)) 
                                          (update % :xp - cost)
                                          %) (:pilots @app-data)))
      (reset! app-state nil) 
      ))
   
(defn- upgrade-modal []
   [:div#upgrade-modal.modal.fade
      [:div.modal-dialog.modal-dialog-centered
         [:div.modal-content
            [:div.modal-header
               [:h5.modal-title "Purchase Upgrade"]
               [:button.close {:data-dismiss "modal"}
                  [:span "x"]]]
                  
            [:div.modal-body
               [:div.row
                  [:div.col-7
                     [:select.custom-select {:size "12"
                                          :on-click (fn [e] 
                                             (let [name (-> e .-target .-value)
                                                  upgrade (->> @app-state 
                                                           :upgradelist
                                                           (filter #(= name (:name %)))
                                                           first)]
                                                   (if (<= (:points upgrade) (:xp @app-state))
                                                       (swap! app-state assoc :canbuy true :selected upgrade)
                                                       (swap! app-state dissoc :canbuy :selected))))}
                        (for [ug (:upgradelist @app-state)]
                           ^{:key ug}[:option {:on-click #(swap! app-state assoc :img (str "images/" (:image ug)))}
                                       (str (:name ug))])]]
                  [:div.col
                     [:img.img-upgrade.border-secondary.rounded {:src (:img @app-state) :hidden (nil? (:img @app-state))}]]]]
                  
            [:div.modal-footer
               [:button.btn.btn-secondary {:data-dismiss "modal"} "Cancel"]
               [:button.btn.btn-primary {:disabled (not (:canbuy @app-state))
                                      :on-click #(buy-upgrade) 
                                      :data-dismiss "modal"} "Upgrade!"]]]]])

(defn- render-pilot [pilot]
   ^{:key (:id pilot)}
   [:div.container-fluid.border-left.border-danger.mt-2.mb-2
      [:div.row-fluid
         ;;[:div (str pilot)]
         [:div.col-sm-12
            [:div.input-group.mb-1
               [:div.input-group-prepend
                  [:span.input-group-text "Callsigns"]]
               [:input.form-control {:placeholder "Your Callsign here"
                                  :value (:callsign pilot)
                                  :on-change #(update-pilot (:id pilot) :callsign (-> % .-target .-value))}]
               [:input.form-control.col-3 {:placeholder "Pilot name"
                                  :value (:player pilot)
                                  :on-change #(update-pilot (:id pilot) :player (-> % .-target .-value))}]
               [:button.btn.btn-danger.ml-1
                  {:data-toggle "modal" 
                   :data-target "#confirm-modal" 
                   :on-click #(setmodal-delete-pilot pilot)} "x"]]
            [:div.form-inline
               [:div.input-group.mb-1.mr-1
                  [:div.input-group-prepend 
                     [:span.input-group-text "Ship"]]
                  [:span.form-control (-> pilot :ship :name)]
                  [:div.input-group-append
                     [:span.input-group-text.btn.btn-secondary "+"]]]
               [:div.input-group.mb-1.mr-1
                  [:div.input-group-prepend
                     [:span.input-group-text "XP"]]
                  [:input.form-control {:type "number" 
                          :value (:xp pilot)
                          :on-change #(update-pilot (:id pilot) :xp (-> % .-target .-value int))}]]
               [:div.input-group.mb-1
                  [:div.input-group-prepend
                     [:span.input-group-text "PS"]]
                  [:input.form-control {:type "number" 
                          :value (:ps pilot)
                          :on-change (fn [e]
                                       (update-pilot (:id pilot) :ps (-> e .-target .-value))
                                       ;; Add Slots
                                       )}]
                  [:div.input-group-append
                     [:span.input-group-text.btn.btn-secondary "+"]]]]]]
      [:div.row-fluid
         [:div.col-sm-6
            (doall 
               (map (fn [upgrade] (input-group-select (:id pilot) upgrade (->> pilot :upgrades (filter #(= (:slot upgrade) (:slot %))))))
                  (:upgradeslots pilot))) ]]])
                  
(defn- draw-page [ws-ch]
   [:div.container.bg-light
      (ship-modal)
      (confirm-modal)
      (upgrade-modal)
      (navbar ws-ch)
      (if (some? @app-data) (render-squad-header))
      (doall (map #(render-pilot %) (:pilots @app-data)))])

;; For testing app state
;;(def info 
;;   {:squadname nil
;;    :pilots [(new-pilot (first ds/ships))]
;;   })
;;
;;(defn render-page [data]
;;   (reset! app-data data)
;;   ;;(swap! app-data assoc-in [:pilots 0 :upgrades] [{:id "2-0" :slot "Astromech" :name "R2 Astromech" :xws "r2astromech" :slotid 0}
;;   ;;                                           {:id "2-1" :slot "Astromech" :name "R2 Astromech" :xws "r2astromech"}
;;   ;;                                           ])
;;   (r/render [draw-page] (.getElementById js/document "app")))


;;(defonce run-once ;; do not reload on figwheel
   (go
      (let [{:keys [ws-channel error]} (<! (ws-ch ws-uri))]
         (if-not error
            (do 
               (loop []
                  (r/render [draw-page ws-channel] (.getElementById js/document "app"))
                  (when-let [{:keys [message]} (<! ws-channel)]
                     (reset! app-data message)
                     (recur))))
          (r/render [:div [:p (str "Error connecting to the server " error)]] (.getElementById js/document "app")))))
;;)













