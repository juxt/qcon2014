;; Copyright © 2013, JUXT LTD. All Rights Reserved.
(ns qcon.web
  (:require
   [bidi.bidi :refer (->Redirect ->Resources ->Files) :as bidi]
   [jig.bidi :refer (add-bidi-routes)]
   [clojure.java.io :as io]
   [hiccup.core :refer (html h)]
   [jig.util :refer (satisfying-dependency)]
   [ring.util.response :refer (resource-response)]
   [liberator.core :refer (defresource)]
   qcon.examples
   jig)
  (:import
   (jig Lifecycle)
   (java.io LineNumberReader InputStreamReader PushbackReader))
  )

(defn index-page [req]
  (resource-response "index.html"))

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

#_(println (source-fn (find-var (symbol "qcon.examples/example-1"))))
#_(println (source-fn (find-var (symbol "qcon.examples/take-rnd-no"))))

(defresource source-resource []
  :available-media-types #{"text/html" "text/plain"}
  :handle-ok (fn [{{mtype :media-type} :representation req :request}]
               (let [text (source-fn (find-var (symbol (:query-string req))))]
                 (case mtype
                   "text/plain"
                   text
                   "text/html"
                   (html [:pre text])))))

(defn make-handlers []
  (let [p (promise)]
    @(deliver p {:index index-page
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

(deftype Website [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (-> system
        (add-bidi-routes config (make-routes config (make-handlers)))))
  (stop [_ system] system))
