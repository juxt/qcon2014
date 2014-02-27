(ns qcon.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :refer [<! >! chan put! sliding-buffer close! pipe map< filter<]]
   [ajax.core :refer (GET POST)]))

(enable-console-print!)

(println "Hello QCon!!!!")

(def app-model {:slides
                [{:title "Introduction"}
                 {:title "Slide 1"}
                 {:title "Slide 2"}
                 {:title "Slide 3"}]})

(defn slide [data owner]
  (om/component
   (dom/p nil "SLIDE 1")))

(defn slides [data owner]
  (om/component
   (dom/div nil
            (om/build slide data))))

(defn nav [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil (dom/p nil "NAV")))))

(om/root slides app-model {:target (.getElementById js/document "content")})
