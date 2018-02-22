(ns user
  (:require [reloaded.repl :refer [system reset stop]]
           [hotac.system]))

(reloaded.repl/set-init! #'hotac.system/create-system)