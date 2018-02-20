(defproject hotac "0.1.0-SNAPSHOT"
	:description  "Pilot Builder and Log for Heroes of the Aturi Cluster"
	:url         "https://github.com/danurai/hotac"
	:license      {:name "Eclipse Public License"
				     :url "http://www.eclipse.org/legal/epl-v10.html"}
				     
	:min-lein-version "2.7.1"

	:dependencies  [[org.clojure/clojure "1.9.0-beta4"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async  "0.3.443"]
                 [reagent "0.7.0"]]

	:plugins      [[lein-figwheel "0.5.14"]
                 [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

	:source-paths  ["src"]

   :cljsbuild     {:builds [{:id "dev"
                           :source-paths ["src"]
                           :figwheel true
                           :compiler {:main hotac.core
                                     :asset-path "js/compiled/out"
                                     :output-to "resources/public/js/compiled/hotac.js"
                                     :output-dir "resources/public/js/compiled/out"
                                     :source-map-timestamp true
                                     :preloads [devtools.preload]}}
                                    
                                    
                         {:id "min"
                          :source-paths ["src"]
                          :compiler {:output-to "resources/public/js/compiled/hotac.js"
                                    :main hotac.core
                                    :optimizations :advanced
                                    :pretty-print false}}]}

  :figwheel     {:css-dirs ["resources/public/css"]} ;; watch and update CSS

  :profiles     {:dev {:plugins [[lein-autoexpect "1.9.0"]]
                     :dependencies [[binaryage/devtools "0.9.4"]
                                  [figwheel-sidecar "0.5.14"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [expectations "2.2.0-rc3"]]
                     ;; need to add dev source path here to get user.clj loaded
                     :source-paths ["src" "dev"]
                     ;; need to add the compliled assets to the :clean-targets
                     :clean-targets ^{:protect false} ["resources/public/js/compiled" :target-path]}})