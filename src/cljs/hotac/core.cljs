(ns hotac.core
   (:require [reagent.core :as r]
             [hotac.model :as model]
             [hotac.communication :as communication]))
        
(enable-console-print!)
(goog-define ws-uri "ws://localhost:9009/ws")

(def app-state (r/atom nil))

;(def upgradedata (js->clj (.parse js/JSON ug/upgrade-json) :keywordize-keys true))
;(def upgradedata (js->clj (.parse js/JSON communication/load-upgrades) :keywordize-keys true))
   
   
;; Load Squad Modal
(defn- load-modal []
   (let [squads (:squads @app-state)]
      [:div#load-squad.modal.fade {:aria-hidden "true" :role "dialog"}
         [:div.modal-dialog 
            [:div.modal-content
               [:div.modal-header 
                  [:h5.modal-title "Load Squad"]
                  [:button.close {:data-dismiss "modal"} [:span "x"]]]
               [:div.modal-body
                 [:div (-> @app-state first)]
                 ;(for [squad @app-state]
                 ;   [:div (-> squad :data str)])
                    ]]]]))
   
;; Ship Modal Functions
   
(defn- set-upgrade-ids [slots]
  (reduce concat
    (for [[k v] (frequencies slots)] (map (fn [n] {:id (str (subs k 0 3) n) :slot k}) (range 0 v)))))
    
(defn- new-ship [pilot ship]
  (prn pilot)
   (-> pilot
      (assoc :ship (dissoc ship :info)
            :upgradeslots (set-upgrade-ids (reduce conj (:slots ship) (:extraslots pilot))))
      (update :xp - (:cost ship))))
    
(defn- new-pilot [ship]
  {:xp (:startxp ship)
   :id (if (= 0 (->> @model/app-data :pilots count))
          1
          (->> @model/app-data :pilots (map :id) (apply max) inc))
   :ps 2
   :ship (dissoc ship :info)
   :upgradeslots (set-upgrade-ids (conj (:slots ship) "Modification")) ; Plus XP?
   :extraslots ["Modification" "Elite"] ;increase with PS
   :upgrades []})

(defn- swap-ship [pilot ship]
  (swap! model/app-data assoc :pilots
    (map #(if (= (:id %) (:id pilot))
             (new-ship pilot ship)
             %) (-> @model/app-data :pilots))))
   
(defn- add-pilot [ship]
   (swap! model/app-data update :pilots conj (new-pilot ship)))   

(defn ship-button-click [pilot ship]
  (if (nil? (:id pilot))
      (add-pilot ship)
      (swap-ship pilot ship))
  (model/squad-change!))
            
(defn- statline [stats]
   (let [statsymbols {:ps "x" :attack "%" :evade "^" :hull "&" :shield "*"}]
      (for [stat stats]
         ^{:key stat}[:span {:class (str "xwing-" (name (key stat)))}
            [:span {:class "xwing-font-b mr-1"} (val stat)]
            [:span {:class "xwing-symbols mr-1"} ((key stat) statsymbols)]])))
            
(defn- ship-button [ship pilot]
   ^{:key ship}
   [:button.btn.btn-outline-secondary
      {:data-dismiss "modal"
       :on-click #(ship-button-click pilot ship)
       :disabled (> (:ps ship) (:ps pilot))}
      ;[:div.small 
      ;  [:span (if (some? (:id pilot)) (str "Min PS: " (:ps ship) " XP Cost: " (:cost ship)))]
      ;  (if (contains? (-> pilot :ship :recommended) (:name ship))
      ;      [:span.float-right.text-warning {:title "Recommended Upgrade!"} [:i.fas.fa-star]])]
      [:span.h3
         [:span.h1.xwing-ships.mr-1 (:symbol ship)]
         [:span.xwing-font-b (:name ship)]
         [:span.badge.badge-secondary.ml-2 (statline (select-keys ship [:attack :evade :hull :shields]))]]
      [:p.font-italic.small.xwing-btn-wrap (:info ship)]])
      
(defn- ship-modal []
   (let [shiplist (:shiplist @app-state (take 2 @model/ships))
         pilot     (:pilot @app-state {:ps 2 :xp 5})]
   [:div#select-ship.modal.fade {:aria-hidden "true" :role "dialog"}
      [:div.modal-dialog
         [:div.modal-content
            [:div.modal-header
               [:h5.modal-title "Select a ship"]
               [:button.close {:data-dismiss "modal"}
                  [:span "x"]]]
            [:div.modal-body
               (doall (for [ship shiplist] (ship-button ship pilot)))]]]]))
               
;; Confirm Modal

(defn- do-modal-action 
"Confirm Modal OK Click Handler"
  []
   (let [action (-> @model/app-data :confirm :action)]
      (case (:type action)
         :delete-pilot  (do (swap! model/app-data assoc :pilots (->> @model/app-data :pilots (remove #(= (:idx action) (:id %)))))
                            (model/squad-change!))
         :default)
      (swap! model/app-data dissoc :actions)))
      
(defn- confirm-modal []
   [:div#confirm-modal.modal.fade
      [:div.modal-dialog
         [:div.modal-content
            [:div.modal-header
               [:h5.modal-title "Please Confirm"]
               [:button.close {:data-dismiss "modal"}
                  [:span "x"]]]
            [:div.modal-body
               [:span.h6 (-> @model/app-data :confirm :message)]]
            [:div.modal-footer
               [:button.btn.btn-secondary {:data-dismiss "modal"} "Close"]
               [:button.btn.btn-warning {:on-click #(do-modal-action) :data-dismiss "modal"} "Affirmative"]]]]])
 
;; Upgrade Modal 
  
(defn- buy-upgrade []
   (let [pilotid (-> @app-state :pilotid)
        cost    (-> @app-state :selected :points)
        upgradeid (str (-> @app-state :selected :id) 
                     "-"
                     (->> @model/app-data :pilots (filter #(= pilotid (:id %))) first :upgrades (filter #(= (-> @app-state :selected :name) (:name %))) count))
        upgrade (-> @app-state :selected (select-keys [:name :slot :xws]) (assoc :slotid (:slotid @app-state) :id upgradeid) )]
        
      (swap! model/app-data assoc :pilots (map #(if (= pilotid (:id %)) 
                                          (update % :upgrades conj upgrade)
                                          %) (:pilots @model/app-data)))
      (swap! model/app-data assoc :pilots (map #(if (= pilotid (:id %)) 
                                          (update % :xp - cost)
                                          %) (:pilots @model/app-data)))
      (reset! app-state nil)))
 
(defn- upgrade-select-click [e]
  (let [name (-> e .-target .-value)
      upgrade (->> @app-state 
               :upgradelist
               (filter #(= name (:name %)))
               first)]
       (if (<= (:points upgrade) (:xp @app-state))
           (swap! app-state assoc :canbuy true :selected upgrade)
           (swap! app-state dissoc :canbuy :selected))))
 
(defn- upgrade-modal []
   [:div#upgrade-modal.modal.fade
      [:div.modal-dialog
         [:div.modal-content
            [:div.modal-header
               [:h5.modal-title "Purchase Upgrade"]
               [:button.close {:data-dismiss "modal"} [:span "x"]]]
            [:div.modal-body
               [:div.row
                  [:div.col-7
                     [:select.custom-select {:size "12" :on-click #(upgrade-select-click %)}
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
               
(defn- navbar []
   [:nav.navbar.navbar-dark.bg-dark.justify-content-between.mb-2
      [:div.container
         [:span.navbar-brand.h1 "HotAC - Squad Builder"]
         [:span   
            [:button.btn.btn-primary.mr-2 
               {:data-toggle "modal"
                :data-target "#load-squad"}
               [:i.fas.fa-chevron-circle-down.mr-2] "Load Squad"]
            [:button.btn.btn-success {:on-click model/new-squad!} [:i.fas.fa-plus.mr-2] "New Squad"]]]])
      
(defn- render-squad-header []
   [:div.row.mb-3.justify-content-between
     [:div.input-group.col-sm-8
        [:div.input-group-prepend
           [:span.input-group-text "Squad Name"]]
        [:input.form-control 
           {:placeholder "Name your squad" 
            :value (:squadname @model/app-data)
            :on-change (fn [e] (swap! model/app-data assoc :squadname (-> e .-target .-value) )(model/squad-change!))}]
        [:div.input-group-append
           ]]
     [:div.col-auto
        [:button.btn.btn-primary.mr-2 {:data-toggle "modal" 
                                       :data-target "#select-ship" 
                                       :on-click #(reset! app-state nil)}
                                      [:i.fas.fa-user-astronaut.mr-2]
                                      "Add Pilot"]
        [:button.btn.btn-warning {:on-click #(communication/save-squad!)} [:i.fas.fa-bookmark.mr-2] "Save Squad"]]
     ])

(defn- setmodal-delete-pilot [pilot]
   (swap! model/app-data assoc :confirm {:message (str "Are you sure you want to permanently delete the " (:type pilot) " pilot " (if (some? (:callsign pilot)) (:callsign pilot) "with no name") "?")
                                :action {:type :delete-pilot :idx (:id pilot)}}))

(defn- update-pilot [id key value]
   (swap! model/app-data assoc :pilots (map #(if (= id (:id %)) (assoc % key value) % ) (:pilots @model/app-data) )))
   
(defn- assign-upgrade [pilotid upgradeid slotid]
   (swap! model/app-data assoc :pilots (model/assign-upgrade-slot pilotid upgradeid slotid (:pilots @model/app-data))))
 
(defn- input-group-select [pilotid upgrade options]
   ^{:key (:id upgrade)}[:div.input-group.mb-1
      [:div.input-group-prepend
         [:span.input-group-text.xwing-symbols (model/slot-symbol (:slot upgrade))]]
      [:select.form-control {:value (:name (->> @model/app-data 
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
                          (if (nil? @model/upgrades) (communication/load-upgrades!))
                          (reset! app-state {:upgradelist (->> @model/upgrades (filter #(= (:slot upgrade) (:slot %))))
                                             :pilotid pilotid
                                             :xp (->> @model/app-data :pilots (filter #(= pilotid (:id %))) first :xp)
                                             :slotid (:id upgrade)}))} "+"]]])
 
(defn- render-pilot [pilot]
   ^{:key (:id pilot)}
   [:div.container-fluid.border-left.border-danger.mt-2.mb-2
      [:div.row-fluid
         ;;[:div (str pilot)]
         [:div.col-sm-12
            [:div.input-group.mb-1
               [:div.input-group-prepend
                  [:span.input-group-text "Callsign"]]
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
                     [:span.input-group-text.btn.btn-secondary 
                        {:data-toggle "modal"
                         :data-target "#select-ship"
                         :on-click #(reset! app-state {:shiplist @model/ships
                                                       :pilot pilot})}
                     "+"]]]
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
                                       (update-pilot (:id pilot) :ps (-> e .-target .-value int))
                                       ;; Add Slots
                                       )}]
                  [:div.input-group-append
                     [:span.input-group-text.btn.btn-secondary "+"]]]]]]
      [:div.row-fluid
         [:div.col-sm-6
            (doall 
               (map (fn [upgrade] (input-group-select (:id pilot) upgrade (->> pilot :upgrades (filter #(= (:slot upgrade) (:slot %))))))
                  (:upgradeslots pilot))) ]]])
                  
(defn- draw-page []
   [:div.bg-light
      [:button.btn {:on-click #(prn @model/app-data)} "data"]
      [:button.btn {:on-click #(prn @model/ships)} "ships"]
      [:button.btn {:on-click #(prn @app-state)} "app-state"]
      (load-modal)
      (ship-modal)
      (confirm-modal)
      (upgrade-modal)
      (navbar)
      [:div.container
         (if (some? @model/app-data)
             (render-squad-header))
         (doall (for [pilot (:pilots @model/app-data)]
            (render-pilot pilot)) )]])

;;(defonce run-once ;; do not reload on figwheel
  (r/render [draw-page] (.getElementById js/document "app"))
;;)













