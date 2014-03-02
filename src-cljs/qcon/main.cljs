(ns qcon.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [om.core :as om :include-macros true]
   [goog.events.KeyCodes :as kc]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [ankha.core :as ankha]
   [cljs.core.async :refer [<! >! buffer chan put! sliding-buffer close! pipe map< filter<]]
   [ajax.core :refer (GET POST)]))

(enable-console-print!)

(def debug false)

(defn builder [data owner]
  [:p "hello, i've been built by a builder"])

(def bufsize 7)

(def buf1 (buffer bufsize))
(def chan1 (chan buf1))

(def default-font "72pt")

(println "Hello mod!" (mod 10 7))

(defn diagram-1 [data owner]
  [:div
   [:svg {:version "1.1" :width 800 :height 600}
    ;;[:rect {:x 0 :y 0 :width 800 :height 600 :fill "#333"}]

    [:g {:transform "translate(70,65)"
         :onClick (fn [_]
                        (go
                          (>! chan1 (str (rand-int 10)))
                          (om/set-state! owner :modified (new js/Date))
                          (.dir js/console buf1)))}

     [:rect {:x 0 :y 0 :width 140 :height 100 :fill "black"}]
     [:text {:x 30 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} ">!"]]

    (for [x (range bufsize)]
      (let [radius 50]
        [:g {:transform (str "translate(320,320)")}
         [:g {:transform (str "rotate(" (- (* (- x (/ bufsize 2) (- 1)) (/ 180 bufsize))) ") translate(200)")}
          [:circle {:cx 0 :cy radius :r radius :style {:fill "#224"}}]
          [:text {:x (- 0 (/ radius 2) 5) :y (* 1.7 radius) :style {:font-size default-font :fill "white"}}
           (str (aget (.-arr (.-buf buf1)) (mod (+ x (.-head (.-buf buf1))) bufsize)))]]]))

    [:g {:transform "translate(70,475)"
         :onClick (fn [_]
                        (go
                          (<! chan1)
                          (om/set-state! owner :modified (new js/Date))))}
     [:rect {
             :x 0 :y 0 :width 140 :height 100 :fill "black"}]
     [:text {:x 30 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} "<!"]]]])


(def app-model
  (atom {:current-slide 5
         :slides
         [{:title "core.async"}

          {:title "Why?"
           ;;:text "Here is the first slide"
           ;;:background "/static/cspdiag.jpg"
           :content [:ul
                     {:li "One"}
                     {:li "Two"}]}

          {:title "What?"}

          {:title "Buffers"
           :code "(<! (chan))"}

          {:title "Diagram"
           :builder builder}

          {:title "put and take"
           :builder diagram-1}

          {:title "When?"}]}))

(defn slide [data owner current]
  (reify
    om/IRender
    (render [_]
      (html
       (if-let [bg (:background data)]
         [:img {:class (str "slide " (:class data)) :src bg
                :style {:width "100%"
                        :height "100%"}}]


         [:section {:class (str "slide " (:class data))}
          [:div {:class "deck-slide-scaler"}
           (when-let [bg (:background data)]
             {:style {:background-image bg
                      :background-repeat "no-repeat"
                      :background-position "center"
                      :background-size "cover"
                      :overflow "hidden"
                      :width "100%"
                      :height "100%"}})

           (if-not (or (:content data) (:builder data))
             [:h1 (:title data)]
             [:h2 (:title data)]
             )

           (when-let [builder (:builder data)]
             (builder data owner)
             )

           #_[:p (:text data)]
           #_(when-let [content (:content data)]
             (apply vec content)
             )
           (when-let [code (:code data)]
             [:pre code]
             )
           #_(when (:custom data)
               [:svg {:version "1.1" :width 600 :height 600}
                [:text {:x 200 :y 100} "(>! (chan))"]
                [:rect {:x 0 :y 0 :width 200 :height 200 :style {:fill "blue"}}]
                [:rect {:x 50 :y 20 :width 100 :height 300 :style {:fill "red"}}]]
               )]
          ])))))

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
