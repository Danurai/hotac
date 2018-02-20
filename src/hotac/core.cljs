(ns hotac.core
   (:require [reagent.core :as r]
            [hotac.datastore :as ds]))
   
(enable-console-print!)

(def app-data (r/atom nil))
(def app-state (r/atom nil))


(defn- new-pilot [ship]
  {:xp (:startxp ship)
   :id (if (= 0 (->> @app-data :pilots count))
          0
          (->> @app-data :pilots (map :id) (apply max) inc))
   :ps 2
   :ship ship
   :upgradeslots (map-indexed (fn [idx x] {:id idx :type x}) (:slots ship))
   })
           
;;[{:symbol "A" :options ["R2 Astromech" "R5-X3"]}] 
           
(defn- statline [stats]
   (let [statsymbols {:ps "x" :attack "%" :evade "^" :hull "&" :shield "*"}]
      (for [stat stats]
         ^{:key stat}[:span {:class (str "xwing-" (name (key stat)))}
            [:span {:class "xwing-font-b mr-1"} (val stat)]
            [:span {:class "xwing-symbols mr-1"} ((key stat) statsymbols)]])))

(def move-symbols [4 7 8 9 6 2])            
(defn- movedial [moves]
   [:table.table-dark [:tbody
      (for [line (reverse moves)]
         ^{:key line}[:tr 
            (for [move (zipmap move-symbols line)]
               ^{:key move}[:td {:class (str "move move-" (val move))} (key move)])])]])

(defn- new-squad []
   (reset! app-data {:squadname nil :pilots []}))
   
(defn- add-pilot [ship]
   (swap! app-data update :pilots conj (new-pilot ship)))
   
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
               
(defn- navbar []
   [:nav.navbar.navbar-dark.bg-dark.justify-content-between.mb-2
      [:span.navbar-brand.h1 "HotAC - Squad Builder"]
      [:button.btn.btn-primary {:on-click new-squad} "New Squad"]])
      
(defn render-squad-header []
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

(defn- assign-upgrade [pilotid slot upgradeid upgrades]
   (let [pidx (->> @app-data :pilots (take-while #(not= 0 (:id %))) count)]
      (swap! app-data assoc-in [:pilots pidx :upgrades] (map #((if (= upgradeid (:id %)) (assoc % :slot slot))
                                                         (if (= slot (:slot %)) (dissoc % :slot))) upgrades))))
   
(defn- input-group-select [pid upgrade options]
   ^{:key (:id upgrade)}[:div.input-group.mb-1
      [:div.input-group-prepend
         [:span.input-group-text.xwing-symbols (ds/slot-symbol (:type upgrade))]]
      [:select.form-control {:value (->> options (filter #(= (:slot %) (:id upgrade))) first :name) 
                          :defaultValue (str "No " (:type upgrade) " selected")
                          :on-change #(assign-upgrade pid (:id upgrade) (.-id (aget (-> % .-target .-options) (-> % .-target .-selectedIndex))) options)}
         (for [opt (concat [{:name (str "No " (:type upgrade) " selected") :default true}] options)]
            ^{:key opt}[:option {:hidden (:default opt) :id (:id opt)}
                        (:name opt) ])]
      [:div.input-group-append
         [:span.input-group-text.btn.btn-secondary "+"]]])
  
(defn render-pilot [pilot]
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
                          :on-change #(update-pilot (:id pilot) :xp (-> % .-target .-value))}]]
               [:div.input-group.mb-1
                  [:div.input-group-prepend
                     [:span.input-group-text "PS"]]
                  [:input.form-control {:type "number" 
                          :value (:ps pilot)
                          :on-change #(update-pilot (:id pilot) :ps (-> % .-target .-value))}]
                  [:div.input-group-append
                     [:span.input-group-text.btn.btn-secondary "+"]]]]]]
      [:div.row-fluid
         [:div.col-sm-6
            (doall 
               (map (fn [upgrade] (input-group-select (:id pilot) upgrade (->> pilot :upgrades (filter #(= (:type upgrade) (:type %))))))
                  (:upgradeslots pilot))) ]]])
      
(defn draw-page []
   [:div.container.bg-light
      (ship-modal)
      (confirm-modal)
      (navbar)
      (if (some? @app-data) (render-squad-header))
      (doall (map #(render-pilot %) (:pilots @app-data)))])

;; For testing app state
(def info 
   {:squadname nil
    :pilots [(new-pilot (last ds/ships))]})

(defn render-page []
   (reset! app-data info)
   (swap! app-data assoc-in [:pilots 0 :upgrades] [{:id 0 :type "Astromech" :name "R2 Astromech" :xws "r2astromech" :slot 0}
                                              {:id 1 :type "Astromech" :name "R3-X5"}
                                              {:id 2 :type "Torpedo" :name "Proton Torpedos" :slot 1}
                                              {:id 7 :type "Torpedo" :name "Ion Torpedos"}])
   (r/render [draw-page] (.getElementById js/document "app")))


(render-page)