(ns qcon.examples
  (:require [clojure.core.async :refer (chan buffer)]))

(defn example-1 []
  (chan))

(defn example-2 []
  (chan 10))

(defn example-3 []
  (chan (buffer)))
