;; Copyright © 2013, JUXT LTD. All Rights Reserved.

;; This is a slide presentation using ClojureScript and Om. Please feel
;; free to make use of this code for your own presentations.

(ns qcon.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
;;   qcon.snippets
   [om.core :as om :include-macros true]
   [goog.events.KeyCodes :as kc]
   [om.dom :as dom :include-macros true]
   [sablono.core :as html :refer-macros [html]]
   [ankha.core :as ankha]
   [cljs.core.async :refer [<! >! timeout buffer chan put! sliding-buffer close! pipe map< filter<]]
   [clojure.string :as string]
   [ajax.core :refer (GET POST)]))

(enable-console-print!)

(def debug false)

(def diagram-width 480)
(def diagram-height 580)

(def svg-attrs
  {:version "1.1" :width diagram-width :height diagram-height})

(defn border []
  [:rect {:x 0 :y 0 :width diagram-width :height diagram-height :stroke "#888" :stroke-width 1 :fill "black"}])

(defn source-snippet [data owner {}]
  (reify
    om/IWillMount
    (will-mount [_]
      (when-let [source (get-in data [:code :source])]
        (case (get-in data [:code :lang])
          :clojure
          (do
            (println "source = " (str "/source?" source))
            (GET (str "/source?" source)
                (-> {:handler (fn [e]
                                (println "e is" e)
                                (om/set-state! owner
                                               :text e))
                     :headers {"Accept" "text/plain"}
                     :response-format :raw})))
          (GET (str "/js/" source)
              (-> {:handler (fn [e]
                              (om/set-state! owner
                                             :text (if-let [[from to] (get-in @data [:code :range])]
                                                     (->>
                                                      (string/split-lines e)
                                                      (drop (dec from))
                                                      (take (- to from))
                                                      (interpose "\n")
                                                      (apply str))
                                                     e)))
                   :headers {"Accept" "text/plain"}
                   :response-format :raw}))))

      (when-let [literal (get-in data [:code :literal])]
        (om/set-state! owner :text literal)
        )
      )
    om/IRender
    (render [_]
      (html
       [:div {:style {:float "right" :width (if (:custom data) "50%" "100%")
                      :font-size (or (get-in data [:code :font-size]) "20pt")} }]))
    om/IDidUpdate
    (did-update [this prev-props prev-state]
      (when (om/get-state owner :text)
        (let [n (om/get-node owner)]
          (while (.hasChildNodes n)
            (.removeChild n (.-lastChild n))))
        (let [pre (.createElement js/document "pre")]
          (set! (.-innerHTML pre) (.-value (hljs.highlightAuto (om/get-state owner :text))))
          (.appendChild (om/get-node owner) pre)
          )))))

#_(defn clj-source-snippet [data owner fname]
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
      #_(println "rendering source: " (om/get-state owner :text))
      (html
       [:div]))
    om/IDidUpdate
    (did-update [this prev-props prev-state]
      ;; Attempt at syntax highlighting
      (println "did-update" (om/get-state owner :text))
      (println (.-value (hljs.highlightAuto "(foo)")))

      #_(println (SyntaxHighlighter.getHtml (om/get-state owner :text)))
      (when (om/get-state owner :text)
        (let [n (om/get-node owner)]
          (while (.hasChildNodes n)
            (.removeChild n (.-lastChild n))))
        (let [pre (.createElement js/document "pre")]
          (set! (.-innerHTML pre) (.-value (hljs.highlightAuto (om/get-state owner :text))))
          (.appendChild (om/get-node owner) pre)
          )))))

(defn new-random-pick [owner]
  (go-loop [c 4]
    (om/set-state! owner :pending-put nil)
    (let [n (inc (mod
                  (+
                   (om/get-state owner :pending-put)
                   (inc (rand-int 9)))
                  9))]
      (om/set-state! owner :pending-put n))
    (when (pos? c)
      (<! (timeout 50))
      (recur (dec c)))))

(defn channels-slide [data owner opts]
  (reify
    om/IInitState
    (init-state [_]
      (let [buf (buffer (:buffer-size opts))
            ch (chan buf)]
        {:buffer-size (:buffer-size opts)
         :buf buf
         :ch ch
         :default-font (:font-size opts)
         :radius (:radius opts)
         :pending-put nil}))
    om/IRender
    (render [_]
      (let [bufsize (om/get-state owner :buffer-size)
            buf (om/get-state owner :buf)
            ch (om/get-state owner :ch)
            default-font (om/get-state owner :default-font)
            radius (om/get-state owner :radius)]
        (html
         [:div
          [:svg svg-attrs
           (border)

           [:g {:transform "translate(0,0)"}

            ;; Random box
            (when (:put data)
              (list

               [:g {:transform "translate(30,0)"
                    :onClick (fn [_] (new-random-pick owner))}
                [:rect
                 {:x 0 :y 65 :width 100 :height 100 :fill "black" :stroke "white" :stroke-width 3}]
                (when-let [n (om/get-state owner :pending-put)]
                  [:text {:x 20 :y 150 :style {:font-size "64pt"
                                               :color "white"} :fill "white"} (str n)])]

               ;; Put
               [:g {:transform "translate(160,65)"
                    :onClick (fn [_]
                               (when-let [n (om/get-state owner :pending-put)]
                                 (om/set-state! owner :pending-put nil)
                                 (go
                                   (>! ch n)
                                   (new-random-pick owner)
                                   ;; Forces a re-render
                                   (om/set-state! owner :modified (new js/Date)))))}
                [:rect {:x 0 :y 0 :width 140 :height 100 :fill "black"}]
                [:text {:x 0 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} ">!"]]))

            ;; Buffer
            (for [x (range bufsize)]
              [:g {:transform (str "translate(320,65)")}
               [:g {:transform (str "translate(0," (* 70 x) ")")}
                [:circle {:cx 0 :cy radius :r radius :style {:fill "#224"}}]
                [:text {:x (- 0 (/ radius 2) 5) :y (* 1.7 radius) :style {:font-size "60pt" :fill "white"}}
                 (str (aget (.-arr (.-buf buf)) x))]]])

            ]]])))))

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
         :radius (:radius opts)
         :pending-put nil}))
    om/IRender
    (render [_]
      (let [bufsize (om/get-state owner :buffer-size)
            buf (om/get-state owner :buf)
            ch (om/get-state owner :ch)
            default-font (om/get-state owner :default-font)
            radius (om/get-state owner :radius)]
        (html
         [:div
          ;;(om/build source-snippet data {})

          [:svg svg-attrs
           (border)

           [:g {:transform "translate(0,0)"}

            ;; Random box
            [:g {:transform "translate(30,0)"
                 :onClick (fn [_] (new-random-pick owner))}
             [:rect
              {:x 0 :y 65 :width 100 :height 100 :fill "black" :stroke "white" :stroke-width 3}]
             (when-let [n (om/get-state owner :pending-put)]
               [:text {:x 20 :y 150 :style {:font-size "64pt"
                                            :color "white"} :fill "white"} (str n)])]

            ;;


            ;; Put
            [:g {:transform "translate(160,65)"
                 :onClick (fn [_]
                            (when-let [n (om/get-state owner :pending-put)]
                              (om/set-state! owner :pending-put nil)
                              (go
                                (>! ch n)
                                (new-random-pick owner)
                                ;; Forces a re-render
                                (om/set-state! owner :modified (new js/Date)))))}
             [:rect {:x 0 :y 0 :width 100 :height 100 :fill "black"}]
             [:text {:x 0 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} ">!"]]

            ;; Buffer
            (for [x (range bufsize)]
              [:g {:transform (str "translate(275,320)")}
               [:g {:transform (str "rotate(" (- (* (- x (/ bufsize 2) (- 1)) (/ 180 bufsize))) ") translate(200)")}
                [:circle {:cx 0 :cy radius :r radius :style {:fill "#224"}}]
                [:text {:x (- 0 (/ radius 2) 5) :y (* 1.7 radius) :style {:font-size default-font :fill "white"}}
                 (str (aget (.-arr (.-buf buf)) (mod (+ x (.-head (.-buf buf))) bufsize)))]]])

            (let [ops (:ops data)]

              ;; Take
              [:g {:transform "translate(160,475)"
                   ;; TODO add ops here
                   :onClick (fn [_]
                              (go
                                (let [v (<! (case ops
                                              :map (map< inc ch)
                                              ch))]
                                  (om/set-state! owner :last-get v))
                                (om/set-state! owner :modified (new js/Date))))}

               [:rect {:x 0 :y 0 :width 100 :height 100 :fill "black"}]
               [:text {:x 0 :y 80 :style {:font-size default-font :stroke "white" :fill "white"}} "<!"]

               ])

            ;; Receive box
            [:g  {:transform "translate(30,475)"}
             [:rect
              {:x 0 :y 0 :width 100 :height 100 :fill "black" :stroke "white" :stroke-width 3}]
             (when-let [n (om/get-state owner :last-get)]
               [:text {:x 20 :y 80 :style {:font-size "64pt"
                                           :color "white"} :fill "white"} (str n)])]]]])))))

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
          [:svg svg-attrs
           (border)
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
          [:svg svg-attrs
           (border)
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
        [:svg svg-attrs
         (border)
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

(defn catch-game-player
  [owner {:keys [id slide channel instances position]}]
  (go-loop [n 0]

    (om/set-state! owner :label n)
    (<! channel)

    (let [to (mod (+ id (+ 2 (rand-int (- (count instances) 4))))
                  (count instances))]
      (let [from-pos position
            to-pos (:position (get instances to))
            xdelta (/ (- (first to-pos) (first from-pos)) 18)
            ydelta (/ (- (second to-pos) (second from-pos)) 18)]

        (go-loop [i 0]
          (<! (timeout 30))
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
        [:svg svg-attrs
         (border)
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
                                  :algo catch-game-player
                                  :instances instances} instance)})])))

           (when-let [[x y] (om/get-state owner :message)]
             [:circle {:cx x :cy y :r 10 :fill "yellow" }])

           ]]]]))))

(def app-model
  (atom {:current-slide 24
         :slides
         ;; TODO Add cardinal such that each slide has its own number to avoid react warning
         (vec
          (map-indexed
           (fn [i m] (assoc m :slideno (str "slide-" (inc i))))
           [{:title "Adventures with core.async"
             :text "Malcolm Sparks — QCon 2014"
             }

            {
             :background "/static/IKEA.png"
             }

            {:subtitle "These slides"
             :bullets ["Available in kit-form: https://github.com/juxt/qcon2014"
                       "Available ready assembled: http://qcon.juxt.pro:8000"]
             }

            {:subtitle "Adventures with core.async"
             :event "QCon 2014"
             :author "Malcolm Sparks"
             :company "JUXT — https://juxt.pro"
             :email "malcolm@juxt.pro"
             :twitter "@malcolmsparks"
             }

            {:title "What is core.async?"}

            {:subtitle "What is core.async?"
             ;;:text "Here is the first slide"
             ;;:background "/static/cspdiag.jpg"
             :bullets ["Clojure library released May 2013"
                       "Based on Communicating Sequential Processes"
                       "Available in Clojure and ClojureScript"]
             }



            {:subtitle "Communicating Sequential Processes?"
             :bullets ["Started with a paper in 1978 by Anthony Hoare"
                       "Then a book a few years later"
                       "Sound mathematical basis for concurrency"
                       "Allows programs to be proven against deadlock - show such a proof"]}

            {:background "/static/cspdiag.jpg"}

            {:title "But what is core.async?"}

            {:background "/static/webcam.jpg"}

            {:title "But what is core.async?"}

            {:title "Quick Tutorial"}

            {:title "Warning: Code ahead!"
             :warning true}

            {:background "/static/bus2.jpg"}

            {:title "Warning: Live code ahead!"
             :warning true}

            #_{:subtitle "buffers"
               :code {:source "cljs/core/async.cljs"
                      :range [17 34]}}

            {:subtitle "channels"
             :bullets ["Form a one-way communcation path way between processes"
                       "Supported by buffers"
                       ]
             }

            #_{:subtitle "channels"
               :custom channels-slide
               :code {:literal "(chan)"
                      :font-size "50pt"}
               :opts {:buffer-size 1 :font-size "72pt" :radius 40}}

            {:subtitle "channels"
             :custom channels-slide
             :code {:literal "(chan 7)"
                    :font-size "50pt"}
             :opts {:buffer-size 7 :font-size "72pt" :radius 40}}

            {:subtitle "put and take"
             :bullets ["Send and receive messages"
                       "Can be used for orchestration"
                       "Threaded (blocking) mode versus lightweight mode"]}

            {:subtitle "put"
             :custom channels-slide
             :put true
             :code {:literal2 "(>! (chan 7)
  (inc (rand-int 9)))"
                    :source "qcon.examples/put-rnd-no"
                    :lang :clojure}

             :opts {:buffer-size 7 :font-size "72pt" :radius 40}}

            {:subtitle "put and take"
             :custom put-and-take-slide
             :code {:source "qcon.examples/take-rnd-no"
                    :lang :clojure
                    }
             :opts {:buffer-size 7 :font-size "72pt" :radius 40}}

            #_{:subtitle "core.async code"
               :code {:source "cljs/core/async.cljs"
                      :range [20 40]}
               :opts {:buffer-size 7 :font-size "72pt" :radius 40}}

            {:subtitle "channels ops"
             :bullets ["map< map>"]}

            {:subtitle "put and take with map< inc"
             :custom put-and-take-slide
             :ops :map
             :code {:source "qcon.examples/map-inc"
                    :lang :clojure}
             :opts {:buffer-size 7 :font-size "72pt" :radius 40}}

            {:subtitle "channels ops"
             :bullets ["map< map>"
                       "filter< filter>"
                       "remove< remove>"
                       "mapcat< mapcat>"
                       "pipe split reduce"
                       "onto-chan"
                       "to-chan"
                       ]}

            ;; TODO "put and take with map> dec"
            ;; TODO "put and take with filter

            ;; TODO Now timeouts and alts

            {:subtitle "timeouts"
             :bullets []}

            {:subtitle "timeouts"
             :custom timeout-slide
             :opts {:font-size "72pt"}}

            {:subtitle "alts"
             :bullets []}

            {:subtitle "alts!"
             :custom alts-slide
             :opts {:font-size "30pt"}}

            {:subtitle "go blocks"
             :bullets ["Similar to Go's 'go routines'"
                       "Eliminate callbacks in code"
                       "Implemented as a macro"]}

            {:subtitle "go blocks"
             :custom go-block-slide
             :opts {:width 600 :height 600
                    :circles 7
                    :radius 60 :font-size "40pt"}}

            #_{:subtitle "catch game"
               :custom catch-game-slide
               :opts {:width 600 :height 600
                      :circles 13
                      :radius 30 :font-size "20pt"}}

            {:subtitle "And that's not all!"
             :bullets ["Mixers"
                       "Pub/sub"
                       "https://github.com/clojure/core.async.git"]}

            ;; TODO result of race in alts! between a channel and a timeout

            ;; Go blocks

            ;; Now a Hecuba table (one hour on this, to create a data set for order/orderlines and a diagram)

            {:title "Why core.async?"
             :text "(it's about decoupling)"}

            {:background "/static/queues.jpg"}

            {:blockquote "I don't know, I don't wanna know."
             :author "Rich Hickey"}

            {:blockquote "If we de-couple, we can re-use."
             :author "Malcolm Sparks"}

            {:background "/static/webcam.jpg"}

            {:title "Example projects"
             :text "Some free software projects using core.async"}

            {:subtitle "MastodonC Hecuba"
             :url "https://github.com/mastodonc/kixi.hecuba"
             :bullets ["Uses core.async to wire together Om components"]}

            ;; TODO Add in Clojure Exchange talk on OpenSensors - demo of SSE

            {:subtitle "MQTT Broker"
             :url "https://github.com/OpenSensorsIO/mqtt-broker"
             :bullets ["Combines Netty and core.async"
                       "Micro-implementation for educational purposes"]}

            {:subtitle "Azondi"
             :url "https://github.com/OpenSensorsIO/azondi"
             :bullets ["Builds on MQTT Broker"
                       "Designed for IoT volume traffic"
                       "Uses LMAX disruptor"
                       "Advanced routing logic"]}

            ;; TODO Don't forget to mention Hecuba and Stentor (that they're free software)
            {:title "Q & A"
             :text "(take n (map answer questions))"}

            {:title "Thank you"
             :text "Please evaluate my talk via the mobile app!"}]))}))

(defn slide [data owner current]
  (reify
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
             [:div
              [:h1 (when (:warning data) {:style {:color "red"
                                                  :text-shadow "0 0 50px #fa0, 0 0 3px #fff"}}) title]
              [:p (:text data)]]
             )

           (when-let [quote (:blockquote data)]
             [:div
              [:h1 ""]
              [:blockquote (str "\"" quote "\"")]
              [:p {:style {:text-align "right"}} (:author data)]
              ]
             )

           (when-let [subtitle (:subtitle data)]
             [:h2 subtitle]

             )

           (when-let [url (:url data)]
             [:p [:a {:href url} url]]
             )

           (when-let [event (:event data)]
             [:div {:style {:text-align "center" :margin-top "20pt"}}
              [:h3 event]
              [:h3 (:author data)]
              [:h3 (:company data)]
              [:h3 (:email data)]
              [:h3 (:twitter data)]
              [:h3 [:a {:href (:slides data)} (:slides data)]]
              ]
             )

           (when-let [bullets (:bullets data)]
             [:ul {:style {:font-size "42pt"}}
              (for [b bullets]
                [:li b]
                )]
             )

           (when-let [code (:code data)]
             (om/build source-snippet data {:opts code})
             )

           (when-let [custom (:custom data)]
             (om/build custom data {:opts (:opts data)}))


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
          (= (.-keyCode e) 36)
          (om/update! app :current-slide 0)

          (= (.-keyCode e) 35)
          (om/update! app :current-slide (dec (count (:slides @app))))

          (= (.-keyCode e) 188)
          (om/transact! app :current-slide #(max 0 (- % 5)))

          (= (.-keyCode e) 190)
          (om/transact! app :current-slide #(min (dec (count (:slides @app))) (+ % 5)))


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

        (om/build-all slide
                      (:slides app)
                      {:key :slideno :init-state chans :opts (:current-slide app)})

        (when (pos? (:current-slide app))
          [:p {:style {:position "fixed" :right "10px" :top "10px" :font-size "16pt"}} (str (inc (:current-slide app)) "/" (count (:slides app)))])]))))


(om/root slides app-model {:target (.getElementById js/document "content")})

(when debug
    (om/root ankha/inspector app-model {:target (.getElementById js/document "debug")}))
