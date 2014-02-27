(ns qcon.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :refer [<! >! chan put! sliding-buffer close! pipe map< filter<]]
   [ajax.core :refer (GET POST)]))

(enable-console-print!)

(def app-model
  (atom {:counters [{:title "Introduction"
                     :text "This is the introduction"}
                    {:title "Slide 1"
                     :text "Here is the first slide"}
                    {:title "Slide 2"}
                    {:title "Slide 3"}]}))


(defn slide [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h2 nil (:title data))
               (dom/p nil (:text data))))))

(defn slides [app owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [message chans]}]
      (apply dom/div nil
             (dom/p nil "There are the slides")
             (om/build-all slide (:counters app)
                           {:key :id :init-state chans})))))

(om/root slides app-model {:target (.getElementById js/document "content")})
