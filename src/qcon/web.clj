;; Copyright © 2013, JUXT LTD. All Rights Reserved.
(ns qcon.web
  (:require
   [bidi.bidi :refer (->Redirect ->Resources ->Files) :as bidi]
   [jig.bidi :refer (add-bidi-routes)]
   [clojure.java.io :as io]
   [stencil.core :as stencil]
   [hiccup.core :refer (html h)]
   [jig.util :refer (satisfying-dependency)]
   [liberator.core :refer (defresource)]
   qcon.examples
   jig)
  (:import
   (jig Lifecycle)
   (java.io LineNumberReader InputStreamReader PushbackReader))
  )

(defn index-page [loader plan]
  (assert loader "Loader is nil")
  (fn [req]
    (assert (loader "slides.html") (format "Can't find slides.html, loader is %s" loader))
    {:status 200 :body (stencil/render (loader "slides.html")
                                       {:content ""; (slurp plan)
                                        :title "QCon Presentation"
                                        :main "qcon.main"})}))

(defn source-fn
  "Returns a string of the source code for the given symbol, if it can
  find it.  This requires that the symbol resolve to a Var defined in
  a namespace for which the .clj is in the classpath.  Returns nil if
  it can't find the source.  For most REPL usage, 'source' is more
  convenient.

  Example: (source-fn 'filter)"
  [v]
  (when-let [filepath (:file (meta v))]
    (if-let [res (io/resource filepath)]
      (when-let [strm (.openStream res)]
        (with-open [rdr (LineNumberReader. (InputStreamReader. strm))]
          (dotimes [_ (dec (:line (meta v)))] (.readLine rdr))
          (let [text (StringBuilder.)
                pbr (proxy [PushbackReader] [rdr]
                      (read [] (let [i (proxy-super read)]
                                 (.append text (char i))
                                 i)))]
            (read (PushbackReader. pbr))
            (str text))))
      (throw (ex-info (format "Nil resource: %s" filepath) {})))))

(defresource source-resource []
  :available-media-types #{"text/html" "text/plain"}
  :handle-ok (fn [{{mtype :media-type} :representation}]
               (let [text (source-fn #'qcon.examples/example-1)]
                 (case mtype
                   "text/plain"
                   text
                   "text/html"
                   (html [:pre text])))))

(defn make-handlers [loader plan]
  (let [p (promise)]
    @(deliver p {:index (index-page loader plan)
                 :source-resource (source-resource)})))


(defn make-routes [config handlers]
  ["/"
   [["index.html" (:index handlers)]
    ["" (->Redirect 307 (:index handlers))]
    ["source" (:source-resource handlers)]
    ["deck.js/" (->Files {:dir (:deckjs.dir config)})]
    ["hl.js/" (->Resources {:prefix "hl.js/"})]
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
