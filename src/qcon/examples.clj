;; Copyright © 2013, JUXT LTD. All Rights Reserved.
(ns qcon.examples
  (:require [clojure.core.async :refer (chan buffer >! <! map< timeout go-loop)]))

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

(defn demo-go-loop [set]
  (go-loop []
    (<!
     (timeout
      (+ 1000
         (rand-int 200))))
    (set :label
         (rand-int 10))
    (recur)))

(defn demo-orch [ch others]
  (go-loop [n 0]
    (<! ch)
    (let [to (get others (rand-int (count others)))]
      (>! to "MESSAGE"))
    (recur (inc n))))
