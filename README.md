# QCon slides

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
