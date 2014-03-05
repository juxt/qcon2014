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

(defn put-and-take-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      (let [buf (buffer (:buffer-size opts))
            ch (chan buf)]
        {:buffer-size (:buffer-size opts)
         :buf buf
         :ch ch
         :default-font (:font-size opts)
         :radius (:radius opts)}))
    om/IRender
    (render [_]
      (let [bufsize (om/get-state owner :buffer-size)
            buf (om/get-state owner :buf)
            ch (om/get-state owner :ch)
            default-font (om/get-state owner :default-font)
            radius (om/get-state owner :radius)]
        (html
         [:div
          [:svg {:version "1.1" :width 800 :height 600}
           [:g {:transform "translate(70,65)"
                :onClick (fn [_]
                           (go
                             (>! ch (str (rand-int 10)))
                             ;; Forces a re-render
                             (om/set-state! owner :modified (new js/Date))))}

            [:g {:transform "translate(90,0)"}
             [:rect {:x 0 :y 0 :width 140 :height 100 :fill "black"}]
             [:text {:x 0 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} ">!"]]]

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

            [:g {:transform "translate(90,0)"}
             [:rect {:x 0 :y 0 :width 140 :height 100 :fill "black"}]
             [:text {:x 0 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} "<!"]]

            ]]])))))

(defn timeout-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:default-font (:font-size opts)
       :status "READY"})

    om/IRender
    (render [_]
      (let [default-font (om/get-state owner :default-font)]
        (html
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
            [:text {:x 30 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} "(timeout 2000)"]]]])))))

(defn alts-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:default-font (:font-size opts)
       :status "READY"})
    om/IRender
    (render [_]
      (let [default-font (om/get-state owner :default-font)]
        (html
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
             "(alts! ch (time-out 2000))"]]]])))))

(defn go-block [data owner {:keys [radius algo font-size] :as opts}]
  (reify
    om/IInitState
    (init-state [_] {:label ""})
    om/IWillMount
    (will-mount [_] (algo owner opts))
    om/IRender
    (render [_]
      (html
       [:g
        [:rect {:x (- (/ radius 2)) :y (- (/ radius 2)) :width radius :height radius :stroke "white" :stroke-width "2" :fill "#5F8"}]
        [:text {:x (- 10) :y 20 :style {:font-size font-size}} (str (om/get-state owner :label))]]))))

(defn go-block-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:circles
       (for [n (range (:circles opts))]
         (let [angle (* n (/ (* 2 Math/PI) (:circles opts)))
               offset (* .8 (/ (- (min (:width opts) (:height opts)) (:radius opts)) 2))]
           [(* offset (Math/cos angle))
            (- (* offset (Math/sin angle)))]))})
    om/IRender
    (render [_]
      (html
       [:div
        [:svg {:version "1.1" :width (:width opts) :height (:height opts)}
         [:g
          [:rect {:x 0 :y 0 :width (:width opts) :height (:height opts) :fill "#222"}]
          ;; Center the diagram
          [:g {:transform (str "translate(" (/ (:width opts) 2) "," (/ (:height opts) 2) ")")}
           (for [[x y] (om/get-state owner :circles)]
             [:g {:transform (str "translate(" x "," y ")")}
              (om/build
               go-block data
               {:opts {:radius (:radius opts)
                       :font-size (:font-size opts)
                       :algo (fn [owner]
                               (go-loop []
                                 (<! (timeout (+ 1000 (rand-int 200))))
                                 (om/set-state! owner :label (str (rand-int 10)))
                                 (recur)
                                 ))}})])]]]]))))

(defn catch-game
  [owner {:keys [id slide channel instances position]}]
  (go-loop [n 0]

    (om/set-state! owner :label n)
    (<! channel)

    (let [to (mod (+ id (+ 2 (rand-int (- (count instances) 4))))
                  (count instances))]
      (let [from-pos position
            to-pos (:position (get instances to))
            xdelta (/ (- (first to-pos) (first from-pos)) 18)
            ydelta (/ (- (second to-pos) (second from-pos)) 18)
            ]

        (go-loop [i 0]
          (<! (timeout 1))
          (om/set-state!
           slide :message
           [(+ (first from-pos) (* i xdelta))
            (+ (second from-pos) (* i ydelta))])
          (if (= i 18)
            (do
              (om/set-state! slide :message nil)
              (>! (get-in instances [to :channel]) "MESSAGE"))
            (recur (inc i)))
          )))
    (recur (inc n))))

(defn catch-game-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:instances
       (vec
        (for [n (range (:circles opts))]
          (let [angle (* n (/ (* 2 Math/PI) (:circles opts)))
                offset (* .8 (/ (- (min (:width opts) (:height opts)) (:radius opts)) 2))]
            {:id n
             :position [(* offset (Math/cos angle))
                        (- (* offset (Math/sin angle)))]
             :channel (chan)
             }
            )))})

    om/IWillMount
    (will-mount [_]
      (go
        (>! (:channel (first (om/get-state owner :instances)))
            "MESSAGE")))

    om/IRender
    (render [_]
      (html
       [:div
        [:svg {:version "1.1" :width (:width opts) :height (:height opts)}
         [:g
          [:rect {:x 0 :y 0 :width (:width opts) :height (:height opts) :fill "#222"}]
          ;; Center the diagram
          [:g {:transform (str "translate(" (/ (:width opts) 2) "," (/ (:height opts) 2) ")")}

           ;; Draw the paths
           (for [[x1 y1] (map :position (om/get-state owner :instances))
                 [x2 y2] (map :position (om/get-state owner :instances))]
             [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2 :stroke "#222" :stroke-width 3}])

           ;; Draw the go-blocks
           (let [instances (om/get-state owner :instances)]
             (for [{:keys [id position channel] :as instance} instances]
               (let [[x y] position]
                 [:g {:transform (str "translate(" x "," y ")")}
                  (om/build
                   go-block data
                   {:opts (merge {:radius (:radius opts)
                                  :font-size (:font-size opts)
                                  :slide owner
                                  :algo catch-game
                                  :instances instances} instance)})])))

           (when-let [[x y] (om/get-state owner :message)]
             [:circle {:cx x :cy y :r 10 :fill "yellow" }])

           ]]]]))))
(def app-model
  (atom {:current-slide 9
         :slides
         ;; TODO Add cardinal such that each slide has its own number to avoid react warning
         (vec
          (map-indexed
           (fn [i m] (assoc m :slideno (str "slide-" (inc i))))
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

            ;; TODO Add source code on right hand side of slide
            {:subtitle "put and take"
             :custom put-and-take-slide
             :opts {:buffer-size 7 :font-size "72pt" :radius 50}}

            {:subtitle "timeouts"
             :custom timeout-slide
             :opts {:font-size "72pt"}}

            {:subtitle "buffers (TODO)"
             :code "(<! (chan))"}

            {:subtitle "alts!"
             :custom alts-slide
             :opts {:font-size "30pt"}}

            {:subtitle "go blocks"
             :custom go-block-slide
             :opts {:width 600 :height 600
                    :circles 7
                    :radius 60 :font-size "40pt"}
             }

            {:subtitle "catch game"
             :custom catch-game-slide
             :opts {:width 600 :height 600
                    :circles 13
                    :radius 30 :font-size "20pt"}
             }

            ;; TODO Play catch between go blocks

            ;; TODO Show source code from cljs sources on disk - via cljs - use different namespaces

            ;; TODO result of race in alts! between a channel and a timeout

            ;; Go blocks

            {:title "When?"}

            ;; TODO Don't forget to mention Hecuba and Stentor (that they're free software)

            {:title "END"}]))}))

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
    #_om/IInitState
    #_(init-state [_]
      (when-let [custom (:custom data)]
        (init-slide-state (om/value custom))))
    om/IRender
    (render [_]
      (html
       (if-let [bg (:background data)]
         [:img {:class (str "slide " (:class data))
                :src bg
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
             (om/build custom data {:opts (:opts data)}))

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

          ;; Hit 0, for beginning
          (= (.-keyCode e) 188)
          (om/update! app :current-slide 0)

          (= (.-keyCode e) 190)
          (om/update! app :current-slide (dec (count (:slides @app))))

          (or (= (.-keyCode e) 37)
              (= (.-keyCode e) kc/PAGE_UP))
          (when (pos? (:current-slide @app))
            (om/transact! app :current-slide dec))

          (or (= (.-keyCode e) 39)
              (= (.-keyCode e) kc/PAGE_DOWN))
          (when (get-in @app [:slides (inc (:current-slide @app))])
            (om/transact! app :current-slide inc))

          :otherwise (println "Keyword is" (.-keyCode e)))

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
                      {:key :slideno :init-state chans :opts (:current-slide app)})]))))


(om/root slides app-model {:target (.getElementById js/document "content")})

(when debug
    (om/root ankha/inspector app-model {:target (.getElementById js/document "debug")}))
