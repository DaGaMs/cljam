(ns cljam.util.sequence
  (:require [clojure.string :as cstr])
  (:import [java.nio Buffer CharBuffer]))

(def ^:private revcomp-table
  (let [ba (byte-array 128)]
    (dotimes [i 128] (aset-byte ba i (byte \N)))
    (doseq [s ["AT" "GC" "NN"]
            [a b] [(cstr/upper-case s) (cstr/lower-case s)]]
      (aset-byte ba (int a) (byte b))
      (aset-byte ba (int b) (byte a)))
    ba))

(defn revcomp
  "Returns a reverse-complement sequence of given sequence. All characters other
  than ATGCNatgcn will be replaced with N. Masks will be kept."
  [^String s]
  (let [l (.length s)
        cb (CharBuffer/allocate l)]
    (dotimes [i l]
      (->> (unchecked-dec (- l i))
           (.charAt s)
           unchecked-int
           (aget ^bytes revcomp-table)
           unchecked-char
           (.put cb)))
    (.flip ^Buffer cb)
    (.toString cb)))
