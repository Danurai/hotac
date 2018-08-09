(ns hotac.model
  (:require
    [reagent.core :as r]))


(def app-data    (r/atom nil))
(def upgrades    (r/atom nil))
(def ships       (r/atom nil))
(def slot-symbol {"Astromech" "A" "Torpedo" "P" "Modification" "m" "Turret" "U" "System" "S" "Cannon" "C" "Crew" "W" "Tech" "X" "Elite" "E" "Missile" "M"})

(defn new-squad! []
   (reset! app-data {:squadname nil :pilots []}))
   
(defn squad-change! []
   (swap! app-data dissoc :saved))
   
(defn- assign-slot [slotid upgradeid upgrades]
   (->> upgrades
         (map #(if (= slotid (:slotid %))
                  (dissoc % :slotid)
                  %))
         (map #(if (= upgradeid (:id %))
                  (assoc % :slotid slotid)
                  %))))

(defn assign-upgrade-slot [pilotid upgradeid slotid pilots]
   (map #(if (= pilotid (:id %))
            (assoc % :upgrades (assign-slot slotid upgradeid (:upgrades %)))
            %) pilots))