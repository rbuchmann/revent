(ns revent.streaming-collections)

(defn apply-changes [m added deleted]
  (apply dissoc
         (into m added)
         (keys deleted)))

(defn apply-changes* [stream-map]
  (let [added (.-added stream-map)
        deleted (.-deleted stream-map)
        m (.-m stream-map)]
    (apply-changes m added deleted)))

(deftype StreamingMap [m added deleted]
    clojure.lang.IPersistentMap
  (assoc [_ k v]
    (StreamingMap. m
                   (assoc added  k v)
                   (dissoc deleted k)))
  (assocEx [_ k v]
    (StreamingMap. m
                   (.assocEx added k v)
                   (dissoc deleted k)))
  (without [_ k]
    (let [v (get added k)]
      (StreamingMap.  m
                      (dissoc added k)
                      (assoc deleted k v))))

  java.lang.Iterable
  (iterator [this]
    (.iterator (apply-changes* this)))

  clojure.lang.Associative
  (containsKey [this k]
    (.containsKey (apply-changes* this) k))
  (entryAt [this k]
    (.entryAt (apply-changes* this) k))

  clojure.lang.IPersistentCollection
  (count [this]
    (.count (apply-changes* this)))
  (cons [this o]
    (StreamingMap. (.cons m o)
                   added
                   deleted))
  (empty [this]
    (.empty (apply-changes* this)))
  (equiv [this o]
    (and (isa? (class o) StreamingMap)
         (.equiv (apply-changes* this) (.(apply-changes* this) o))))

  clojure.lang.Seqable
  (seq [this]
    (.seq (apply-changes* this)))

  clojure.lang.ILookup
  (valAt [this k]
    (.valAt (apply-changes* this) k))
  (valAt [this k not-found]
    (.valAt (apply-changes* this) k not-found)))

(defn streaming-map [m]
  (StreamingMap. m {} {}))

(defn splat [m]
  [(.-m m) (.-added m) (.-deleted m)])
