(ns qcon.web
  (:require
   [jig.bidi :refer (add-bidi-routes)]
   jig)
  (:import (jig Lifecycle))
  )

(defn index-page []
  (fn [req]
    {:status 200 :body "Hello QCon!"}))

(defn make-handlers []
  (let [p (promise)]
    @(deliver p {:index (index-page)})))

(defn make-routes [handlers]
  ["/index" (:index handlers)])

(deftype Website [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (-> system
        (add-bidi-routes config (make-routes (make-handlers)))))
  (stop [_ system] system))
