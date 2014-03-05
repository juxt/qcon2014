;; Copyright © 2013, JUXT LTD. All Rights Reserved.

;; This is a slide presentation using ClojureScript and Om. Please feel
;; free to make use of this code for your own presentations.

(ns qcon.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [om.core :as om :include-macros true]
   [goog.events.KeyCodes :as kc]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [ankha.core :as ankha]
   [cljs.core.async :refer [<! >! timeout buffer chan put! sliding-buffer close! pipe map< filter<]]
   [ajax.core :refer (GET POST)]))

(enable-console-print!)

(def debug false)

;; A protocol for custom slides

(defprotocol Slide
  (init-slide-state [_])
  (render-slide [_ data owner]))

(defrecord PutAndTakeSlide [opts]
  Slide
  (init-slide-state [_]
    (let [buf (buffer (:buffer-size opts))
          ch (chan buf)]
      {:buffer-size (:buffer-size opts)
       :buf buf
       :ch ch
       :default-font (:font-size opts)
       :radius (:radius opts)}))
  (render-slide [_ data owner]
    (let [bufsize (om/get-state owner :buffer-size)
          buf (om/get-state owner :buf)
          ch (om/get-state owner :ch)
          default-font (om/get-state owner :default-font)
          radius (om/get-state owner :radius)]
      [:div
       [:svg {:version "1.1" :width 800 :height 600}
        [:g {:transform "translate(70,65)"
             :onClick (fn [_]
                        (go
                          (>! ch (str (rand-int 10)))
                          ;; Forces a re-render
                          (om/set-state! owner :modified (new js/Date))))}

         [:rect {:x 0 :y 0 :width 140 :height 100 :fill "black"}]
         [:text {:x 30 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} ">!"]]

        (for [x (range bufsize)]
          [:g {:transform (str "translate(320,320)")}
           [:g {:transform (str "rotate(" (- (* (- x (/ bufsize 2) (- 1)) (/ 180 bufsize))) ") translate(200)")}
            [:circle {:cx 0 :cy radius :r radius :style {:fill "#224"}}]
            [:text {:x (- 0 (/ radius 2) 5) :y (* 1.7 radius) :style {:font-size default-font :fill "white"}}
             (str (aget (.-arr (.-buf buf)) (mod (+ x (.-head (.-buf buf))) bufsize)))]]])

        [:g {:transform "translate(70,475)"
             :onClick (fn [_]
                        (go
                          (<! ch)
                          (om/set-state! owner :modified (new js/Date))))}
         [:rect {
                 :x 0 :y 0 :width 140 :height 100 :fill "black"}]
         [:text {:x 30 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} "<!"]]]])))

(defrecord TimeoutSlide [opts]
  Slide
  (init-slide-state [_]
    {:default-font (:font-size opts)
     :status "READY"})

  (render-slide [_ data owner]
    (let [default-font (om/get-state owner :default-font)]
      [:div
       [:svg {:version "1.1" :width 800 :height 600}
        [:text {:x 30 :y 120 :style {:font-size default-font :stroke "white" :fill "white"}} (om/get-state owner :status)]
        [:g {:transform "translate(70,150)"
             :onClick (fn [_]
                        (om/set-state! owner :status "WAITING")
                        (go
                          (<! (timeout 2000))
                          (om/set-state! owner :status "CLOSED")))}
         [:rect {:x 0 :y 0 :width 280 :height 100 :fill "red"}]
         [:text {:x 30 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} "(timeout 2000)"]]]])))


(defrecord AltsSlide [opts]
  Slide
  (init-slide-state [_]
    {:default-font (:font-size opts)
     :status "READY"})
  (render-slide [_ data owner]
    (let [default-font (om/get-state owner :default-font)]
      [:div
       [:svg {:version "1.1" :width 800 :height 600}
        [:text {:x 30 :y 120 :style {:font-size default-font :stroke "white" :fill "white"}} (om/get-state owner :status)]
        [:g {:transform "translate(70,150)"
             :onClick (fn [_]
                        (om/set-state! owner :status "WAITING")
                        (go
                          (<! (timeout 2000))
                          (om/set-state! owner :status "CLOSED")))}
         [:rect {:x 0 :y 0 :width 280 :height 100 :fill "red"}]
         [:text {:x 30 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}}
          "(alts! ch (time-out 2000))"]]]])))

(defn create-circle [offset angle]
  (fn [app owner]
    (reify
      om/IInitState
      (init-state [_]
        {:label (rand-int 10)}
        )
      om/IWillMount
      (will-mount [_]
        (go-loop []
          (<! (timeout (+ 1000 (rand-int 200))))
          (om/set-state! owner :label (str (rand-int 10)))
          (recur)
          )
        )
      om/IRender
      (render [_]
        (html
         (let [x (+ 300 (* offset (Math/cos angle)))
               y (- 300 (* offset (Math/sin angle)))
               x2 (+ 300 (* (inc offset) (Math/cos angle)))
               y2 (- 300 (* (inc offset) (Math/sin angle)))
               ]
           [:g {:transform (str "translate(" x "," y ")")}
            [:circle {:cx 0 :cy 0 :r 30 :fill "#5F8"}]
            [:text {:x -10 :y 10 :style {:font-size "32pt"}} (str (om/get-state owner :label))]]
))))))

(defrecord GoBlockSlide [opts]
  Slide
  (init-slide-state [_]
    (let [circles (:circles opts)]
      {:default-font (:font-size opts)
       :circles (for [n (range circles)]
                  (let [angle (* n (/ (* 2 Math/PI) circles))
                        offset 250]
                    (create-circle offset angle)))}))

  (render-slide [_ data owner]
    (let [default-font (om/get-state owner :default-font)]
      [:div
       [:svg {:version "1.1" :width 600 :height 600}
        [:g
         [:rect {:x 0 :y 0 :width 600 :height 600 :fill "#292"}]
         (for [c (om/get-state owner :circles)]
           (om/build c data))

         #_[:text {:x 20 :y 120 :fill "#f00" :style {:font-size "80pt"}} "Hello Ruben, buon compleano!!!"]
         ]
        ]])))

(def app-model
  (atom {:current-slide 8
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

          {:subtitle "channels (TODO)"}

          {:subtitle "put and take"
           :custom (PutAndTakeSlide. {:buffer-size 7 :font-size "72pt" :radius 50})}

          {:subtitle "timeouts"
           :custom (TimeoutSlide. {:font-size "72pt"})}

          {:subtitle "buffers (TODO)"
           :code "(<! (chan))"}

          {:subtitle "alts!"
           :custom (AltsSlide. {:font-size "30pt"})}

          {:subtitle "go blocks"
           :custom (GoBlockSlide. {:font-size "80pt" :circles 5})}
          ;; Show result of race in alts! between a channel and a timeout

          ;; Go blocks

          {:title "When?"}

          {:title "Hello Ruben"}

          {:title "END?"}]}))

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
         (println (.-keyCode e))

         (cond
          (= (.-keyCode e) 49)
          (do
            (println "I am going to the beginning of the presentation!")
            (om/update! app :current-slide 0))

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
