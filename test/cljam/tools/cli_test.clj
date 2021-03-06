(ns cljam.tools.cli-test
  (:require [clojure.test :refer [deftest is are testing use-fixtures]]
            [cljam.test-common :refer
             [with-before-after
              prepare-cache!
              clean-cache!
              not-throw?
              same-sam-contents?
              same-sequence-contents?
              check-sort-order
              slurp-sam-for-test
              slurp-bam-for-test
              disable-log-fixture
              temp-dir
              test-sam-file
              test-bam-file
              test-sam-sorted-by-pos
              test-sam-sorted-by-qname
              test-sorted-bam-file
              test-fa-file
              test-twobit-file
              normalize-before-bam-file
              normalize-after-bam-file
              test-pileup-file
              test-pileup-dir
              test-sorted-bam-levels]]
            [clojure.java.io :as cio]
            [cljam.tools.cli :as cli]
            [cljam.io.sam :as sam])
  (:import [java.io PrintStream]))

(use-fixtures :once disable-log-fixture)

(defmacro with-out-file
  [f & body]
  `(let [os# (cio/output-stream ~f)
         old-ps# System/out
         ps# (PrintStream. os#)]
     (try
       (System/setOut ps#)
       (binding [*out* (cio/writer os#)]
         ~@body)
       (finally
         (.flush ps#)
         (System/setOut old-ps#)))))

(def temp-out (str temp-dir "/out"))
(def temp-bam (str temp-dir "/out.bam"))
(def temp-sam (str temp-dir "/out.sam"))
(def temp-fasta (str temp-dir "/out.fa"))
(def temp-twobit (str temp-dir "/out.2bit"))

(deftest about-view
  (with-before-after {:before (prepare-cache!)
                      :after (clean-cache!)}
    ;; NB: "view" output format may change in future
    (are [args] (not-throw? (with-out-file temp-out (cli/view args)))
      [test-sam-file]
      ["-f" "sam" test-sam-file]
      [test-bam-file]
      ["-f" "bam" test-bam-file]
      ["--header" test-bam-file]
      ["-r" "ref2" test-sorted-bam-file]
      ["-r" "ref2:10-200" test-sorted-bam-file])))

(deftest about-convert
  (with-before-after {:before (prepare-cache!)
                      :after (clean-cache!)}
    (testing "SAM -> BAM"
      (is (not-throw? (cli/convert [test-sam-file temp-bam])))
      (is (= (slurp-bam-for-test temp-bam) (slurp-sam-for-test test-sam-file)))
      (is (= (slurp-bam-for-test temp-bam) (slurp-bam-for-test test-bam-file))))
    (testing "BAM -> SAM"
      (is (not-throw? (cli/convert [test-bam-file temp-sam])))
      (is (= (slurp-sam-for-test temp-sam) (slurp-bam-for-test test-bam-file)))
      (is (= (slurp-sam-for-test temp-sam) (slurp-sam-for-test test-sam-file))))
    (testing "FASTA -> TwoBit"
      (is (not-throw? (cli/convert [test-fa-file temp-twobit])))
      (is (same-sequence-contents? test-fa-file temp-twobit)))
    (testing "TwoBit -> FASTA"
      (is (not-throw? (cli/convert [test-twobit-file temp-fasta])))
      (is (same-sequence-contents? test-twobit-file temp-fasta)))
    (testing "error"
      (are [in out] (thrown? Exception (cli/convert [in out]))
        test-bam-file (str temp-dir "/test.unknown")
        test-bam-file temp-twobit))))

(deftest about-normalize
  (with-before-after {:before (prepare-cache!)
                      :after (clean-cache!)}
    (is (not-throw? (with-out-file temp-out (cli/normalize [normalize-before-bam-file temp-bam]))))
    (is (same-sam-contents? temp-bam normalize-after-bam-file))))

(deftest about-sort-by-pos
  (with-before-after {:before (prepare-cache!)
                      :after (clean-cache!)}
    (is (not-throw? (with-out-file temp-out (cli/sort ["-o" "coordinate" test-sam-file temp-sam]))))
    (is (= (slurp-sam-for-test temp-sam) test-sam-sorted-by-pos))
    (is (not-throw? (check-sort-order (slurp-sam-for-test temp-sam) test-sam-sorted-by-pos)))
    (is (not-throw? (with-out-file temp-out (cli/sort ["-o" "coordinate" "-c" "4" test-bam-file temp-bam]))))
    (is (= (slurp-bam-for-test temp-bam) test-sam-sorted-by-pos))
    (is (not-throw? (check-sort-order (slurp-bam-for-test temp-bam) test-sam-sorted-by-pos)))
    (is (thrown? IllegalArgumentException (with-out-file temp-out (cli/sort ["-o" "coordinate" test-fa-file temp-bam]))))
    (is (thrown? IllegalArgumentException (with-out-file temp-out (cli/sort ["-o" "coordinate" test-bam-file (str temp-dir "/test.unknown")]))))))

(deftest about-sort-by-qname
  (with-before-after {:before (prepare-cache!)
                      :after (clean-cache!)}
    (is (not-throw? (with-out-file temp-out (cli/sort ["-o" "queryname" test-sam-file temp-sam]))))
    (is (= (slurp-sam-for-test temp-sam) test-sam-sorted-by-qname))
    (is (not-throw? (with-out-file temp-out (cli/sort ["-o" "queryname" test-bam-file temp-bam]))))
    (is (= (slurp-bam-for-test temp-bam) test-sam-sorted-by-qname))))

(deftest about-index
  (with-before-after {:before (do (prepare-cache!)
                                  (cio/copy (cio/file test-sorted-bam-file)
                                            (cio/file temp-bam)))
                      :after (clean-cache!)}
    (is (not-throw? (with-out-file temp-out (cli/index [temp-bam]))))
    (is (.exists (cio/file (str temp-bam ".bai"))))
    (is (not-throw? (with-out-file temp-out (cli/index ["-t" "1" temp-bam]))))
    (is (not-throw? (with-out-file temp-out (cli/index ["-t" "4" temp-bam]))))))

(deftest about-pileup
  (with-before-after {:before (prepare-cache!)
                      :after (clean-cache!)}
    ;; NB: "pileup" output format may change in future (maybe)
    (is (not-throw? (with-out-file temp-out (cli/pileup [test-sorted-bam-file]))))
    (is (= (slurp temp-out) (slurp test-pileup-file)))
    (let [bam-file test-sorted-bam-file]
      (doseq [t ["1" "4"]]
        (are [?args ?file] (= (do
                                (with-out-file temp-out (cli/pileup ?args))
                                (slurp temp-out))
                              (slurp (str test-pileup-dir ?file)))
          ["-t" t "-s" bam-file] "s.pileup"
          ["-t" t "-f" test-fa-file bam-file] "f.pileup"
          ["-t" t "-s" "-f" test-fa-file bam-file] "sf.pileup"
          ["-t" t "-r" "ref2" bam-file] "r1.pileup"
          ["-t" t "-r" "ref2" "-s" bam-file] "r1s.pileup"
          ["-t" t "-r" "ref2" "-f" test-fa-file bam-file] "r1f.pileup"
          ["-t" t "-r" "ref2" "-s" "-f" test-fa-file bam-file] "r1sf.pileup"
          ["-t" t "-r" "ref2:10-200" bam-file] "r2.pileup"
          ["-t" t "-r" "ref2:10-200" "-s" bam-file] "r2s.pileup"
          ["-t" t "-r" "ref2:10-200" "-f" test-fa-file bam-file] "r2f.pileup"
          ["-t" t "-r" "ref2:10-200" "-s" "-f" test-fa-file bam-file] "r2sf.pileup")))))

(deftest about-faidx
  (with-before-after {:before (do (prepare-cache!)
                                  (cio/copy (cio/file test-fa-file)
                                            (cio/file temp-out)))
                      :after (clean-cache!)}
    (is (not-throw? (with-out-file temp-out (cli/faidx [temp-out]))))
    (is (.exists (cio/file (str temp-out ".fai"))))))

(deftest about-dict
  (with-before-after {:before (prepare-cache!)
                      :after (clean-cache!)}
    (let [temp-dict (str temp-dir "/out.dict")]
      (is (not-throw? (with-out-file temp-out (cli/dict [test-fa-file temp-dict]))))
      (is (.exists (cio/file temp-dict))))))

(deftest about-level
  (with-before-after {:before (prepare-cache!)
                      :after (clean-cache!)}
    (is (thrown? clojure.lang.ExceptionInfo
                 (with-out-file temp-out (cli/level [test-bam-file temp-bam]))))
    (is (not-throw? (with-out-file temp-out (cli/level [test-sorted-bam-file
                                                        temp-bam]))))
    (with-open [rdr (sam/bam-reader temp-bam)]
      (is (= (map #(first (keep :LV (:options %)))
                  (sam/read-alignments rdr))
             test-sorted-bam-levels)))))

(deftest about-run
  (with-before-after {:before (prepare-cache!)
                      :after (clean-cache!)}
    (is (not-throw? (with-out-file temp-out (cli/run ["view" test-sam-file]))))))
