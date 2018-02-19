(ns hotac.core
   (:require [reagent.core :as r]))
   
(enable-console-print!)

(def app-data (r/atom nil))

(def xwing {:type "X-Wing" 
           :attack 3 :evade 2 :hull 3 :shields 2 :actions ["Focus" "Target Lock"]
           :maneuvers [[0,0,0,0,0,0],[0,2,2,2,0,0],[1,1,2,1,1,0],[1,1,1,1,1,0],[0,0,1,0,0,3]]
           :slots ["Modification" "Astromech" "Torpedo"]
           :symbol "x" :xp 5 :info "The X-wing is a well-rounded campaign workhorse, and is great for cleaning up enemy fighters. For Pilots looking to switch to an A-wing or B-wing later, it tends to be the most efficient starting choice."})
(def ywing {:type "Y-Wing" 
           :attack 2 :evade 1 :hull 3 :shields 2 :actions ["Focus" "Target Lock"] 
           :maneuvers [[0,0,0,0,0,0],[0,1,2,1,0,0],[1,1,2,1,1,0],[3,1,1,1,3,0],[0,0,3,0,0,3]]
           :slots ["Modification" "Astromech" "Torpedo" "Torpedo" "Turret"]
           :special ["Title" "BTL-A4"]
           :symbol "y" :xp 8 :info "Capable of carrying a turret and multiple types of ordnance, the Y-wing is flexible enough to take on any campaign mission. Bomb Loadout is factored into the scoresheet and not limited here, and BTL-A4 can give any turreted Y-wing extra punch."})

(defn- new-pilot [ship]
   (prn (->> @app-data :pilots count))
   (assoc ship :id (if (= 0 (->> @app-data :pilots count))
                     0
                     (->> @app-data :pilots (map :id) (apply max) inc))
              :callsign nil
              :ps 2))
           
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
   (reset! app-data {:squadname nil :pilots []})
   (prn app-data))
   
(defn- add-pilot [ship]
   (swap! app-data update :pilots conj (new-pilot ship)))
   
(defn- ship-button [ship]
   [:button.btn.btn-outline-secondary
      {:data-dismiss "modal"
       :on-click #(add-pilot ship)}
      [:span.h3
         [:span.h1.xwing-ships.mr-1 (:symbol ship)]
         [:span.xwing-font-b (:type ship)]
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
               (ship-button xwing)
               (ship-button ywing)]]]])
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
      [:button.btn.btn-primary {:on-click new-squad} "New Squadron"]])
      
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

(defn render-pilot [idx pilot]
   ^{:key (:id pilot)}[:div.border-left.border-primary.mt-2.mb-2
      [:div.row-fluid
         [:div (str pilot)]
         [:div.col-sm-12
            [:div.input-group.mb-1
               [:div.input-group-prepend
                  [:span.input-group-text "Callsign"]]
               [:input.form-control {:placeholder "Your callsign here" 
                                  :value (:callsign pilot)
                                  :on-change #(swap! app-data update-in [:pilots idx] assoc :callsign (-> % .-target .-value))}]
               [:button.btn.btn-danger.ml-1
                  {:data-toggle "modal" 
                   :data-target "#confirm-modal" 
                   :on-click #(swap! app-data assoc :confirm {:message (str "Are you sure you want to permanently delete the " (:type pilot) " pilot " (if (some? (:callsign pilot)) (:callsign pilot) "with no name") "?")
                                                         :action {:type :delete-pilot :idx (:id pilot)}})}
                  "x"]]]]])
      
(defn draw-page []
   [:div.container
      (ship-modal)
      (confirm-modal)
      (navbar)
      (if (some? @app-data) (render-squad-header))
      (doall (map-indexed (fn [idx pilot] (render-pilot idx pilot)) (:pilots @app-data)))])

;; For testing app state
(def info 
   {:squadname nil
    :pilots []})

(defn render-page []
   (reset! app-data info)
   (r/render [draw-page] (.getElementById js/document "app")))


(render-page)