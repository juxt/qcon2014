;; Copyright © 2013, JUXT LTD. All Rights Reserved.
(ns qcon.examples
  (:require [clojure.core.async :refer (chan buffer >! <! map<)]))

(defn put-rnd-no []
  (let [ch (chan 7)]
    (>! ch
        (inc (rand-int 9)))))


(defn take-rnd-no []
  (let [ch (chan 7)]
    (>! ch
        (inc (rand-int 9)))
    (println (<! ch))
    ))

(defn map-inc []
  (let [ch (chan 7)]
    (>! ch
        (inc (rand-int 9)))
    (<! (map< inc ch))
    ))

(defn example-1 []
  (chan))

(defn example-2 []
  (chan 10))

(defn example-3 []
  (chan (buffer)))
