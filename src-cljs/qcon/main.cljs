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

(def default-font "72pt")

(defprotocol Slide
  (init-slide-state [_])
  (render-slide [_ data owner]))

(defrecord PutAndTakeSlide []
  Slide
  (init-slide-state [_]
    (println "Init state of PutAndTakeSlide")
    (let [bufsize 7
          buf1 (buffer bufsize)
          chan1 (chan buf1)]
      {:bufsize bufsize
       :buf1 buf1
       :chan1 chan1}))
  (render-slide [_ data owner]
    (let [bufsize (om/get-state owner :bufsize)
          buf1 (om/get-state owner :buf1)
          chan1 (om/get-state owner :chan1)]
      [:div
       [:svg {:version "1.1" :width 800 :height 600}

        [:g {:transform "translate(70,65)"
             :onClick (fn [_]
                        (go
                          (>! chan1 (str (rand-int 10)))
                          (om/set-state! owner :modified (new js/Date))))}

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
         [:text {:x 30 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} "<!"]]]])))

(def app-model
  (atom {:current-slide 3
         :slides
         [{:title "core.async"
           :event "QCon 2014"
           :author "Malcolm Sparks"
           :company "JUXT"
           :email "malcolm@juxt.pro"
           :twitter "@malcolmsparks"
           }

          {:subtitle "What is core.async?"
           ;;:text "Here is the first slide"
           ;;:background "/static/cspdiag.jpg"
           :bullets ["Clojure library released May 2013"
                     "Based on Communicating Sequential Processes"
                     "Available in Clojure and ClojureScript"]
           }

          {:title "Quick tutorial"}

          {:subtitle "put and take"
           :custom (PutAndTakeSlide.)}

          {:title "Buffers"
           :code "(<! (chan))"}

          {:title "When?"}]}))

(defn source-snippet [data owner fname]
  (reify
    om/IWillMount
    (will-mount [_]
      (GET "/source"
          (-> {:handler (fn [e]
                          (om/set-state! owner :text e))
               :headers {"Accept" "text/plain"}
               :response-format :raw})))
    om/IRender
    (render [_]
      (html
       [:div]))
    om/IDidUpdate
    (did-update [this prev-props prev-state]
      ;; Attempt at syntax highlighting
      #_(println (sh.Highlighter.getHtml (om/get-state owner :text)))
      (let [n (om/get-node owner)]
        (while (.hasChildNodes n)
          (.removeChild n (.-lastChild n))))
      (let [pre (.createElement js/document "pre")]
        (.setAttribute pre "class" "brush: clojure")
        (.appendChild pre (.createTextNode js/document (om/get-state owner :text)))
        (.appendChild (om/get-node owner) pre)))))

(defn slide [data owner current]
  (reify
    om/IInitState
    (init-state [_]
      (when-let [custom (:custom data)]
        (init-slide-state (om/value custom))))
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

           (when-let [title (:title data)]
             [:h1 title]
             )

           (when-let [subtitle (:subtitle data)]
             [:h2 subtitle]
             )

           (when-let [custom (:custom data)]
             (when (satisfies? Slide (om/value custom))
               (render-slide (om/value custom) data owner)))

           (when-let [event (:event data)]
             [:div {:style {:text-align "center" :margin-top "20pt"}}
              [:h3 event]
              [:h3 (:author data)]
              [:h3 (:company data)]
              [:h3 (:email data)]
              [:h3 (:twitter data)]
              ]
             )

           (when-let [bullets (:bullets data)]
             [:ul {:style {:font-size "42pt"}}
              (for [b bullets]
                [:li b]
                )]
             )

           #_[:p (:text data)]
           #_(when-let [content (:content data)]
             (apply vec content)
             )
           (when-let [code (:code data)]
             (om/build source-snippet data {:opts "filter"})
             )
           ]
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
