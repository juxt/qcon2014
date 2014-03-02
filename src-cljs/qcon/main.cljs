(ns qcon.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [om.core :as om :include-macros true]
   [goog.events.KeyCodes :as kc]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [ankha.core :as ankha]
   [cljs.core.async :refer [<! >! chan put! sliding-buffer close! pipe map< filter<]]
   [ajax.core :refer (GET POST)]))

(enable-console-print!)

(def debug false)

(def app-model
  (atom {:current-slide 1
         :slides
         [{:title "core.async"
           }
          {:title "Why?"
           ;;:text "Here is the first slide"
           ;;           :background "/static/cspdiag.jpg"
           }
          {:title "What?"}
          {:title "Buffers"
           :code "(<! (chan))"
           }
          {:title "Diagram"
           :custom true
           }
          {:title "When?"
           }]}))

(defn slide [data owner current]
  (reify
    om/IRender
    (render [_]
      (html
       [:section {:class (str "slide " (:class data))}
        [:div {:class "deck-slide-scaler"}
         #_(when (:background data)
             {:style {:background "url(/static/cspdiag.jpg)"}})

         [:h1 (:title data)]
         [:p (:text data)]
         (when-let [code (:code data)]
           [:pre code]
           )
         #_(when (:custom data)
           [:svg {:version "1.1" :width 600 :height 600}
            [:text {:x 200 :y 100} "(>! (chan))"]
            [:rect {:x 0 :y 0 :width 200 :height 200 :style {:fill "blue"}}]
            [:rect {:x 50 :y 20 :width 100 :height 300 :style {:fill "red"}}]]
           )]
        ]))))

(defn set-slide-class! [app n max clz]
  (when (and (not (neg? n))
             (< n max))
    (om/update! app [:slides n :class] clz)))

(defn navigate-to-slide! [app n max]
  (doseq [i (range max)]
    (set-slide-class!
     app i max
     (cond
      (= i n) "deck-current"
      (= (- i n) 1) "deck-next"
      (= (- i n) -1) "deck-previous"
      (< i n) "deck-before"
      (> i n) "deck-after"))))

(defn slides [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (goog.events.listen
       js/document "keydown"
       (fn [e]
         (cond
          (or (= (.-keyCode e) 37)
              (= (.-keyCode e) kc/PAGE_UP))
          (when (pos? (:current-slide @app))
            (om/transact! app :current-slide dec))
          (or (= (.-keyCode e) 39)
              (= (.-keyCode e) kc/PAGE_DOWN))
          (when (get-in @app [:slides (inc (:current-slide @app))])
            (om/transact! app :current-slide inc)))

         (navigate-to-slide! app (:current-slide @app) (count (:slides @app)))))

      ;; Navigate to initial slide
      (navigate-to-slide! app (:current-slide app) (count (:slides app)))
      )

    om/IRender
    (render [_]
      (html
       [:div
        #_[:p (str "Current slide is " (inc (:current-slide app)) "/" (count (:slides app)))]
        (om/build-all slide
                      (:slides app)
                      {:key :title :init-state chans :opts (:current-slide app)})]))))


(om/root slides app-model {:target (.getElementById js/document "content")})

(when debug
    (om/root ankha/inspector app-model {:target (.getElementById js/document "debug")}))
