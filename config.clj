{:jig/components
 {
  :stencil-loader
  {
   :jig/component jig.stencil/StencilLoader
   :jig/project "../qcon/project.clj"}

  :website
  {:jig/component qcon.web/Website
   :jig/project "../qcon/project.clj"
   :jig/dependencies [:stencil-loader]
   :deckjs.dir "../deck.js"
   :sh.dir "../syntaxhighlighter/pkg"
   }

  :cljs-builder
  {:jig/component jig.cljs-builder/Builder
   :jig/project "../qcon/project.clj"
   :output-dir "../qcon/target/js"
   :output-to "../qcon/target/js/main.js"
   :source-map "../qcon/target/js/main.js.map"
   :optimizations :none
   ;; :pretty-print true
   }

  :cljs-server
  {:jig/component jig.bidi/ClojureScriptRouter
   :jig/dependencies [:cljs-builder]
   :jig.web/context "/js/"
   }

  :routing
  {:jig/component jig.bidi/Router
   :jig/project "../qcon/project.clj"
   :jig/dependencies [:cljs-server :website]
   ;; Optionally, route systems can be mounted on a sub-context
   ;;:jig.web/context "/services"
   }

  :webserver
  {:jig/component jig.http-kit/Server
   :jig/project "../qcon/project.clj"
   :jig/dependencies [:routing]
   :port 8000}

  }}
