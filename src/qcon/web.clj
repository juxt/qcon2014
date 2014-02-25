(ns qcon.web
  (:require
   [bidi.bidi :refer (->Redirect)]
   [jig.bidi :refer (add-bidi-routes)]
   [stencil.core :as stencil]
   [jig.util :refer (satisfying-dependency)]
   jig)
  (:import (jig Lifecycle))
  )

(defn index-page [loader]
  (assert loader "Loader is nil")
  (fn [req]
    (assert (loader "slides.html") (format "Can't find slides.html, loader is %s" loader))
    {:status 200 :body (stencil/render (loader "slides.html") {:content "Hello QCon!"})}))

(defn make-handlers [loader]
  (let [p (promise)]
    @(deliver p {:index (index-page loader)})))

(defn make-routes [handlers]
  ["/"
   [["index.html" (:index handlers)]
    ["" (->Redirect 307 (:index handlers))]]])

(defn get-template-loader [system config]
  (if-let [{id :jig/id} (satisfying-dependency system config 'jig.stencil/StencilLoader)]
    (if-let [template-loader (get-in system [id :jig.stencil/loader])]
      template-loader
      (throw (ex-info (format "Failed to find lookup template loader in system at path %s"
                              [id :jig.stencil/loader])
                      {:path [id :jig.stencil/loader]})))
    (throw (ex-info (format "Component must depend on a %s component" 'jig.stencil/StencilLoader) {}))
    )
  )

(deftype Website [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (let [loader (get-template-loader system config)]
      (-> system
          (assoc :loader loader)
          (add-bidi-routes config (make-routes (make-handlers loader))))))
  (stop [_ system] system))
