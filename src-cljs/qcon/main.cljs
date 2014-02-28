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
  (atom {:current-slide 0
         :slides [{:title "Introduction"
                   :text "This is the introduction"
                   :visible true}
                  {:title "Slide 1"
                   :text "Here is the first slide"
                   :visible false}
                  {:title "Slide 2"
                   :visible false}
                  {:title "Slide 3"
                   :visible false}]}))


(defn slide [data owner current]
  (reify
    om/IRender
    (render [_]
      (html
       (if (:visible data)
         [:div
          [:h2 {:style {:color "green"}} (:title data)]
          [:p (:text data)]]
         [:div {:style {:display :none}}]
         )))))

(defn slides [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (goog.events.listen js/document "keydown"
                          (fn [e]
                            (cond
                             (= (.-keyCode e) kc/PAGE_UP)
                             (when (pos? (:current-slide @app))
                               (om/update! app [:slides (:current-slide @app) :visible] false)
                               (om/transact! app :current-slide dec)
                               (om/update! app [:slides (:current-slide @app) :visible] true))
                             (= (.-keyCode e) kc/PAGE_DOWN)
                             (when (get-in @app [:slides (inc (:current-slide @app))])
                               (om/update! app [:slides (:current-slide @app) :visible] false)
                               (om/transact! app :current-slide inc)
                               (om/update! app [:slides (:current-slide @app) :visible] true)
                               ))
                            )))
    om/IRender
    (render [_]
      (println "Rendering slides")
      (html
       [:div
        [:p (str "Current slide is " (inc (:current-slide app)))]
        (om/build-all slide
                      (:slides app)
                      {:key :title :init-state chans :opts (:current-slide app)})]))))


(om/root slides app-model {:target (.getElementById js/document "content")})

(when debug
    (om/root ankha/inspector app-model {:target (.getElementById js/document "debug")}))
