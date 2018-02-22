(ns hotac.controller
   (:require ))
        
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


;;(def move-symbols [4 7 8 9 6 2])            
;;(defn- movedial [moves]
;;   [:table.table-dark [:tbody
;;      (for [line (reverse moves)]
;;         ^{:key line}[:tr 
;;            (for [move (zipmap move-symbols line)]
;;               ^{:key move}[:td {:class (str "move move-" (val move))} (key move)])])]])
             
;;(def pilots 
;;   [{:id 1
;;     :ps 2
;;     :xp 8
;;     :ship {:name "Y-Wing"}
;;     :upgradeslots [{:id 0 :type "Astromech"}
;;                  {:id 1 :type "Torpedo"}
;;                  {:id 2 :type "Torpedo"}
;;                  {:id 3 :type "Turret"}]
;;     :upgrades [{:id 0 :type "Astromech" :name "R2 Astromech" :xws "r2astromech" :slot 0}
;;               {:id 1 :type "Astromech" :name "R3-X5"}
;;               {:id 2 :type "Torpedo" :name "Proton Torpedos" :slot 1}
;;               {:id 7 :type "Torpedo" :name "Ion Torpedos"}]}
;;    {:id 2
;;     :ps 2
;;     :xp 5
;;     :ship {:name "X-Wing"}
;;     :upgradeslots [{:id 0 :type "Astromech"}
;;                  {:id 1 :type "Torpedo"}]
;;     :upgrades [{:id "2-0" :type "Astromech" :name "R2 Astromech" :xws "r2astromech" :slotid 0}
;;               {:id "2-1" :type "Astromech" :name "R2 Astromech" :xws "r2astromech"}]}])