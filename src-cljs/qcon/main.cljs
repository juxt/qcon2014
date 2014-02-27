(ns qcon.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [cljs.core.async :refer [<! >! chan put! sliding-buffer close! pipe map< filter<]]
   [ajax.core :refer (GET POST)]))

(enable-console-print!)

(def app-model
  (atom {:current-slide 1
         :slides [{:id 1
                   :title "Introduction"
                   :text "This is the introduction"}
                  {:id 2
                   :title "Slide 1"
                   :text "Here is the first slide"}
                  {:id 3
                   :title "Slide 2"}
                  {:id 4
                   :title "Slide 3"}]}))


(defn slide [data owner current]
  (reify
    om/IRender
    (render [_]
      (html
       [:div {:style {:visibility (if (= current (:id data)) "visible" "hidden")}}
        [:h2 {:style {:color "green"}} (:title data)]
        [:p (:text data)]]))))

(defn slides [app owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [message chans]}]
      (html
       [:div
        (om/build-all slide
                      (:slides app)
                      {:key :id :init-state chans :opts (:current-slide app)})]))))

(om/root slides app-model {:target (.getElementById js/document "content")})
