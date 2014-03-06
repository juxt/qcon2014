;; Copyright © 2013, JUXT LTD. All Rights Reserved.
(ns qcon.examples
  (:require [clojure.core.async :refer (chan buffer >! <! map< timeout)]))

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

(defn demo-timeout [clicks]
  (println "READY")
  (<! clicks)
  (println "WAITING")
  (<! (timeout 2000))
  (println "CLOSED"))
