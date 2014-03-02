(ns qcon.web
  (:require
   [bidi.bidi :refer (->Redirect ->Resources ->Files) :as bidi]
   [jig.bidi :refer (add-bidi-routes)]
   [clojure.java.io :as io]
   [stencil.core :as stencil]
   [jig.util :refer (satisfying-dependency)]
   jig)
  (:import (jig Lifecycle))
  )

(defn index-page [loader plan]
  (assert loader "Loader is nil")
  (fn [req]
    (assert (loader "slides.html") (format "Can't find slides.html, loader is %s" loader))
    {:status 200 :body (stencil/render (loader "slides.html")
                                       {:content ""; (slurp plan)
                                        :title "QCon Presentation"
                                        :main "qcon.main"})}))

(defn make-handlers [loader plan]
  (let [p (promise)]
    @(deliver p {:index (index-page loader plan)})))

(defn make-routes [config handlers]
  ["/"
   [["index.html" (:index handlers)]
    ["" (->Redirect 307 (:index handlers))]
    ["deck.js/" (->Files {:dir (:deckjs.dir config)})]
    ["static/" (->Resources {:prefix ""})]
    ]])

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
    (let [loader (get-template-loader system config)
          plan (io/file (-> config :jig/project :project-file (.getParentFile)) "plan.org")]
      (-> system
          (assoc :loader loader)
          (add-bidi-routes config (make-routes config (make-handlers loader plan))))))
  (stop [_ system] system))
