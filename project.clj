(def jig-version "2.0.2-SNAPSHOT")

(defproject qcon "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :exclusions [
               org.clojure/clojurescript
]

  :dependencies [
                 [org.clojure/clojurescript "0.0-2156"]
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
                 ]

  :source-paths ["src" "src-cljs"]


)
