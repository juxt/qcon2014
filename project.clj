;; Copyright © 2013, JUXT LTD. All Rights Reserved.
(def jig-version "2.0.3")

(defproject qcon "0.1.0-SNAPSHOT"
  :description "Slides for Malcolm Sparks's core.async talk at QCon 2014"
  :url "http://github.com/juxt/qcon"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :exclusions [org.clojure/clojurescript]

  :dependencies [[org.clojure/clojurescript "0.0-2156"]
                 [org.clojure/clojure "1.5.1"]
                 [jig/protocols ~jig-version]
                 [jig/cljs-builder ~jig-version]
                 [jig/http-kit ~jig-version]
                 [jig/bidi ~jig-version]
                 [jig/stencil ~jig-version]
                 [jig/async ~jig-version]
                 [om "0.5.0"]
                 [sablono "0.2.6"]
                 [ankha "0.1.1"]
                 [cljs-ajax "0.2.3"]
                 [liberator "0.11.0"]
                 [hiccup "1.0.5"]
                 ]

  :source-paths ["src" "src-cljs"]

)
