(ns hotac.datastore
   (:require))

(def ships [{:name "X-Wing" 
            :attack 3 
            :evade 2 
            :hull 3 
            :shields 2 
            :actions ["Focus" "Target Lock"]
            :maneuvers [[0,0,0,0,0,0],[0,2,2,2,0,0],[1,1,2,1,1,0],[1,1,1,1,1,0],[0,0,1,0,0,3]]
            :slots ["Astromech" "Torpedo"]
            :symbol "x"
            :startxp 5
            :info "The X-wing is a well-rounded campaign workhorse, and is great for cleaning up enemy fighters. For Pilots looking to switch to an A-wing or B-wing later, it tends to be the most efficient starting choice."}
           {:name "Y-Wing" 
            :attack 2 
            :evade 1 
            :hull 3 
            :shields 2 
            :actions ["Focus" "Target Lock"] 
            :maneuvers [[0,0,0,0,0,0],[0,1,2,1,0,0],[1,1,2,1,1,0],[3,1,1,1,3,0],[0,0,3,0,0,3]]
            :slots ["Astromech" "Torpedo" "Torpedo" "Turret"]
            :special ["Title" "BTL-A4"]
            :symbol "y" 
            :startxp 8 
            :info "Capable of carrying a turret and multiple types of ordnance, the Y-wing is flexible enough to take on any campaign mission. Bomb Loadout is factored into the scoresheet and not limited here, and BTL-A4 can give any turreted Y-wing extra punch."}])

(def slot-symbol {"Astromech" "A" "Torpedo" "P" "Modification" "m" "Turret" "U"})