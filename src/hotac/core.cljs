(ns hotac.core
    (:require
      [reagent.core :as r]))

(enable-console-print!)

(println "This text is printed from src/hotac/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defn- navbar []
   [:nav.navbar.navbar-dark.bg-dark.mb-3
      [:div.container
         [:div.navbar-header
            [:span.navbar-brand "Heroes of the Aturi Cluster - Squadron Builder"]]]])

(defn- input-group [title placeholder value]
   [:div.input-group.mb-1
      [:div.input-group-prepend
         [:span.input-group-text title]]
      [:input.form-control {:placeholder placeholder :value value}]])
      
(defn- input-group-symbol [symbol value options]
   [:div.input-group.mb-1
      [:div.input-group-prepend
         [:span.input-group-text.xwing-symbols symbol]]
      [:select.form-control
         (for [opt options]
            ^{:key opt}[:option opt])]])
      
(defn- input-group-PS-symbol [ps symbol value options]
   [:div.input-group.mb-1
      [:div.input-group-prepend
         [:span.input-group-text
            [:span.xwing-font-b.xwing-ps-upgrade ps]
            [:span.xwing-symbols symbol]]]
      [:select.form-control {:value value}
         (for [opt options]
            ^{:key opt}[:option opt])]])

(defn- input-group-select [title options value]
   [:div.input-group.mb-1
      [:div.input-group-prepend
         [:span.input-group-text title]]
      [:select.form-control {:value value}
         (for [opt options]
            ^{:key opt}[:option opt])]
      [:div.input-group-append
         [:span.input-group-text
            [:input.mr-2 {:type "checkbox"}]
            [:span "BTL-A4"]
            ]]])

(defn- statline [stats]
   (let [statsymbols {:ps "x" :attack "%" :evade "^" :hull "&" :shield "*"}]
      [:div.form-control.form-control-lg.bg-secondary.text-light.mb-1.mr-1
         (for [stat stats]
            ^{:key stat}[:span {:class (str "xwing-" (name (key stat)))}
               [:span {:class "xwing-font-b mr-1"} (val stat)]
               [:span {:class "xwing-symbols mr-1"} ((key stat) statsymbols)]])]))
               
(defn- symbol-line [symbols]
   [:div.form-control.form-control-lg.bg-secondary.text-light.mb-1.mr-1
      (map-indexed (fn [idx sym]
                     ^{:key idx}[:span.xwing-symbols.mr-1 sym]) symbols)])       
         
(defn drawpage []
   [:div.container
      (navbar)
      [:div.row
         [:div.col-sm-12
            [:div.row
               [:div.col-sm-12
                  (input-group "Squadron Name" "Your Squadname Here" nil)]]
            [:div.row
               [:div.col-sm-6
                  (input-group "Callsign" "Blue Leader" nil)
                  (input-group-select "Ship" ["X-Wing" "Y-Wing"] nil)]
               [:div.col-sm-6
                  (input-group "Pilot" "Your Name Here" nil)
                  (input-group "XP" nil nil)]]
            [:div.row
               [:div.col-sm-12
                  [:div.form-inline.mb-1
                     (statline {:ps 2 :attack 2 :evade 1 :hull 5 :shield 3})
                     (symbol-line (vec "tmAPPU"))
                     (symbol-line (vec "fl"))
                     ]]]
            [:div.row
               [:div.col-sm-6
                  (input-group-symbol "A" nil [])
                  (input-group-symbol "U" nil [])
                  (input-group-symbol "m" nil [])
                  (input-group-PS-symbol 4 "m" nil [])
                  (input-group-PS-symbol 6 "m" nil [])
                  (input-group-PS-symbol 8 "m" nil [])]
               [:div.col-sm-6
                  (input-group-symbol "P" nil [])
                  (input-group-symbol "P" nil [])
                  (input-group-PS-symbol 3 "m" nil [])
                  (input-group-PS-symbol 5 "m" nil [])
                  (input-group-PS-symbol 7 "m" nil [])
                  (input-group-PS-symbol 9 "m" nil [])]
               
            ]]]])
      
(r/render [drawpage] (.getElementById js/document "app"))