# QCon slides

There are a number of HTML-based tools out there (reveal.js, slidy, impress.js, deck.js, etc..) for building slide-based presentations for conference talks, etc.

But I find they have a number of disadvantages, at least for me :

1. You usually have to write your slides in raw HTML. This feels cumbersome when you're used to writing LISP s-expressions with paredit.

2. If you want to build in more interactivity, you have to be proficient in JavaScript.

In March 2014 I gave a presentation at QCon in London for which I built
a slide-deck in [Om](https://github.com/swannodette/om).

![slide-19](slide-19.png)

If you're reading this on an old-style computer with a keyboard then you
can view it here http://qcon.juxt.pro/index.html. Use the left/right
arrow keys, and on slide 19 you can click in the black box and on the `>!`
and `<!` symbols, and there are other interactive slides in there to help
explain some core.async concepts with some visualisations.

You write your slides in EDN. For most slides, you can use simple conventions like this :

```clojure
{:title "Hello"
 :bullets ["Point 1" "Point 2" "Point 3"]}
```

You can add a `:custom` key which references a _custom slide_, which is an Om component.

Om seems ideal for slide-decks. Om's component modularity protects the
independence of individual custom slides, so they can be transferred to
other decks easily. I've found that, as a Clojure developer, building
slides this way has been much easier than building them with JavaScript.

If anyone would like to re-use some or all of this code for an upcoming
talk or presentation, please feel free to use anything here if it
helps. If there is enough interest, we could separate the common
functions into a small library.

## Config

```
#=(eval
   (-> (clojure.core/read-string (slurp "/home/malcolm/src/qcon/config.clj"))

       ;; Due to symlinks not working on dropbox, replace all instances
       ;; of relative paths with absolute ones

       ((partial clojure.walk/postwalk
                 (fn [x] (if-let [path (and (string? x) (second (re-matches #"\.\./qcon(.*)" x)))]
                           (str "/home/malcolm/src/qcon" path)
                           x))))

       ((partial clojure.walk/postwalk
                 (fn [x] (if-let [path (and (string? x) (second (re-matches #"\.\./deck.js(.*)" x)))]
                           (str "/home/malcolm/src/deck.js" path)
                           x))))))
```
