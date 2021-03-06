(ns cljam.io.wig-test
  (:require [clojure.test :refer [deftest is are testing]]
            [clojure.java.io :as cio]
            [clojure.string :as cstr]
            [cljam.test-common :refer
             [with-before-after
              prepare-cache!
              clean-cache!
              not-throw?
              http-server
              temp-dir
              test-wig-file1
              test-wig-file2]]
            [cljam.io.wig :as wig])
  (:import [java.io BufferedReader InputStreamReader ByteArrayInputStream
            ByteArrayOutputStream OutputStreamWriter BufferedWriter]
           [cljam.io.wig WIGReader WIGWriter]))

(def ^:private test-wig-fields1
  [{:track {:line "track type=wiggle_0 name=\"variableStep\" description=\"variableStep format\" visibility=full autoScale=off viewLimits=0.0:25.0 color=50,150,255 yLineMark=11.76 yLineOnOff=on priority=10"
            :format :variable-step :chr "chr19" :step nil :span 150}
    :chr "chr19" :start 49304701 :end 49304850 :value 10.0}
   {:track {:line "track type=wiggle_0 name=\"variableStep\" description=\"variableStep format\" visibility=full autoScale=off viewLimits=0.0:25.0 color=50,150,255 yLineMark=11.76 yLineOnOff=on priority=10"
            :format :variable-step :chr "chr19" :step nil :span 150}
    :chr "chr19" :start 49304901 :end 49305050 :value 12.5}
   {:track {:line "track type=wiggle_0 name=\"variableStep\" description=\"variableStep format\" visibility=full autoScale=off viewLimits=0.0:25.0 color=50,150,255 yLineMark=11.76 yLineOnOff=on priority=10"
            :format :variable-step :chr "chr19" :step nil :span 150}
    :chr "chr19" :start 49305401 :end 49305550 :value 15.0}
   {:track {:line "track type=wiggle_0 name=\"variableStep\" description=\"variableStep format\" visibility=full autoScale=off viewLimits=0.0:25.0 color=50,150,255 yLineMark=11.76 yLineOnOff=on priority=10"
            :format :variable-step :chr "chr19" :step nil :span 150}
    :chr "chr19" :start 49305601 :end 49305750 :value 17.5}
   {:track {:line "track type=wiggle_0 name=\"variableStep\" description=\"variableStep format\" visibility=full autoScale=off viewLimits=0.0:25.0 color=50,150,255 yLineMark=11.76 yLineOnOff=on priority=10"
            :format :variable-step :chr "chr19" :step nil :span 150}
    :chr "chr19" :start 49305901 :end 49306050 :value 20.0}
   {:track {:line "track type=wiggle_0 name=\"variableStep\" description=\"variableStep format\" visibility=full autoScale=off viewLimits=0.0:25.0 color=50,150,255 yLineMark=11.76 yLineOnOff=on priority=10"
            :format :variable-step :chr "chr19" :step nil :span 150}
    :chr "chr19" :start 49306081 :end 49306230 :value 17.5}
   {:track {:line "track type=wiggle_0 name=\"variableStep\" description=\"variableStep format\" visibility=full autoScale=off viewLimits=0.0:25.0 color=50,150,255 yLineMark=11.76 yLineOnOff=on priority=10"
            :format :variable-step :chr "chr19" :step nil :span 150}
    :chr "chr19" :start 49306301 :end 49306450 :value 15.0}
   {:track {:line "track type=wiggle_0 name=\"variableStep\" description=\"variableStep format\" visibility=full autoScale=off viewLimits=0.0:25.0 color=50,150,255 yLineMark=11.76 yLineOnOff=on priority=10"
            :format :variable-step :chr "chr19" :step nil :span 150}
    :chr "chr19" :start 49306691 :end 49306840 :value 12.5}
   {:track {:line "track type=wiggle_0 name=\"variableStep\" description=\"variableStep format\" visibility=full autoScale=off viewLimits=0.0:25.0 color=50,150,255 yLineMark=11.76 yLineOnOff=on priority=10"
            :format :variable-step :chr "chr19" :step nil :span 150}
    :chr "chr19" :start 49307871 :end 49308020 :value 10.0}
   {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
            :format :fixed-step :chr "chr19" :step 300 :span 200}
    :chr "chr19" :start 49307401 :end 49307600 :value 1000}
   {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
            :format :fixed-step :chr "chr19" :step 300 :span 200}
    :chr "chr19" :start 49307701 :end 49307900 :value 900}
   {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
            :format :fixed-step :chr "chr19" :step 300 :span 200}
    :chr "chr19" :start 49308001 :end 49308200 :value 800}
   {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
            :format :fixed-step :chr "chr19" :step 300 :span 200}
    :chr "chr19" :start 49308301 :end 49308500 :value 700}
   {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
            :format :fixed-step :chr "chr19" :step 300 :span 200}
    :chr "chr19" :start 49308601 :end 49308800 :value 600}
   {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
            :format :fixed-step :chr "chr19" :step 300 :span 200}
    :chr "chr19" :start 49308901 :end 49309100 :value 500}
   {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
            :format :fixed-step :chr "chr19" :step 300 :span 200}
    :chr "chr19" :start 49309201 :end 49309400 :value 400}
   {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
            :format :fixed-step :chr "chr19" :step 300 :span 200}
    :chr "chr19" :start 49309501 :end 49309700 :value 300}
   {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
            :format :fixed-step :chr "chr19" :step 300 :span 200}
    :chr "chr19" :start 49309801 :end 49310000 :value 200}
   {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
            :format :fixed-step :chr "chr19" :step 300 :span 200}
    :chr "chr19" :start 49310101 :end 49310300 :value 100}])

(def ^:private ^String temp-wig-file (str temp-dir "/test.wig"))

(defn- str->wig
  [^String s]
  (with-open [bais (ByteArrayInputStream. (.getBytes s))
              isr (InputStreamReader. bais)
              br (WIGReader. (BufferedReader. isr) nil)]
    (doall (wig/read-fields br))))

(defn- wig->str
  [xs]
  (with-open [bao (ByteArrayOutputStream.)
              osw (OutputStreamWriter. bao)
              bw (WIGWriter. (BufferedWriter. osw) nil)]
    (wig/write-fields bw xs)
    (.flush ^BufferedWriter (.writer bw))
    (.toString bao)))

(deftest reader
  (testing "make reader instance"
    (with-open [rdr (wig/reader test-wig-file1)]
      (is (instance? cljam.io.wig.WIGReader rdr))))

  (testing "throw Exception"
    (is (thrown? Exception (wig/reader "test-resources/wig/not-found.wig"))))

  (testing "read wig"
    (is (= (str->wig "variableStep chrom=chr1 span=10\n1 1\n101 2")
           [{:track {:line nil :format :variable-step :chr "chr1" :step nil :span 10}
             :chr "chr1" :start 1 :end 10 :value 1}
            {:track {:line nil :format :variable-step :chr "chr1" :step nil :span 10}
             :chr "chr1" :start 101 :end 110 :value 2}]))
    (is (= (str->wig "fixedStep chrom=chr1 start=1 step=20 span=10\n1\n2")
           [{:track {:line nil :format :fixed-step :chr "chr1" :step 20 :span 10}
             :chr "chr1" :start 1 :end 10 :value 1}
            {:track {:line nil :format :fixed-step :chr "chr1" :step 20 :span 10}
             :chr "chr1" :start 21 :end 30 :value 2}])))

  (testing "read wig with default span value for variableStep"
    (is (= (str->wig "variableStep chrom=chr1\n1 1\n101 2")
           [{:track {:line nil :format :variable-step :chr "chr1" :step nil :span 1}
             :chr "chr1" :start 1 :end 1 :value 1}
            {:track {:line nil :format :variable-step :chr "chr1" :step nil :span 1}
             :chr "chr1" :start 101 :end 101 :value 2}])))

  (testing "read wig with default span and step value for fixedStep"
    (is (= (str->wig "fixedStep chrom=chr1 start=1\n1\n2")
           [{:track {:line nil :format :fixed-step :chr "chr1" :step 1 :span 1}
             :chr "chr1" :start 1 :end 1 :value 1}
            {:track {:line nil :format :fixed-step :chr "chr1" :step 1 :span 1}
             :chr "chr1" :start 2 :end 2 :value 2}])))

  (testing "read a single track with a single variableStep"
    (is (= (str->wig (cstr/join \newline ["track type=wiggle_0"
                                          "variableStep chrom=chr1 span=10"
                                          "1 1"]))
           [{:track {:line "track type=wiggle_0" :format :variable-step
                     :chr "chr1" :step nil :span 10}
             :chr "chr1" :start 1 :end 10 :value 1}])))

  (testing "read a single track with a single fixedStep"
    (is (= (str->wig (cstr/join \newline ["track type=wiggle_0"
                                          "fixedStep chrom=chr1 start=1 span=10"
                                          "1"]))
           [{:track {:line "track type=wiggle_0" :format :fixed-step
                     :chr "chr1" :step 1 :span 10}
             :chr "chr1" :start 1 :end 10 :value 1}])))

  (testing "read wig with continuous same formats"
    (let [s (cstr/join \newline ["variableStep chrom=chr19 span=5"
                                 "1 1"
                                 "11 10"
                                 "variableStep chrom=chr19 span=10"
                                 "101 1"
                                 "111 10"])]
      (is (= (str->wig s)
             [{:track {:line nil :format :variable-step :chr "chr19" :step nil :span 5}
               :chr "chr19" :start 1 :end 5 :value 1}
              {:track {:line nil :format :variable-step :chr "chr19" :step nil :span 5}
               :chr "chr19" :start 11 :end 15 :value 10}
              {:track {:line nil :format :variable-step :chr "chr19" :step nil :span 10}
               :chr "chr19" :start 101 :end 110 :value 1}
              {:track {:line nil :format :variable-step :chr "chr19" :step nil :span 10}
               :chr "chr19" :start 111 :end 120 :value 10}])))
    (let [s (cstr/join \newline ["fixedStep chrom=chr19 start=1 step=10 span=5"
                                 "1"
                                 "10"
                                 "fixedStep chrom=chr19 start=101 step=10 span=10"
                                 "1"
                                 "10"])]
      (is (= (str->wig s)
             [{:track {:line nil :format :fixed-step :chr "chr19" :step 10 :span 5}
               :chr "chr19" :start 1 :end 5 :value 1}
              {:track {:line nil :format :fixed-step :chr "chr19" :step 10 :span 5}
               :chr "chr19" :start 11 :end 15 :value 10}
              {:track {:line nil :format :fixed-step :chr "chr19" :step 10 :span 10}
               :chr "chr19" :start 101 :end 110 :value 1}
              {:track {:line nil :format :fixed-step :chr "chr19" :step 10 :span 10}
               :chr "chr19" :start 111 :end 120 :value 10}]))))

  (testing "read wig file"
    (with-open [r (wig/reader test-wig-file1)]
      (is (= (wig/read-fields r)
             test-wig-fields1)))
    (with-open [r (wig/reader test-wig-file2)]
      (is (= (wig/read-fields r)
             [{:track {:line "track type=wiggle_0 name=\"variableStep\" description=\"variableStep format\" visibility=full autoScale=off viewLimits=0.0:25.0 color=50,150,255 yLineMark=11.76 yLineOnOff=on priority=10"
                       :format :variable-step :chr "chr19" :step nil :span 1}
               :chr "chr19" :start 49304701 :end 49304701 :value 10.0}
              {:track {:line "track type=wiggle_0 name=\"variableStep\" description=\"variableStep format\" visibility=full autoScale=off viewLimits=0.0:25.0 color=50,150,255 yLineMark=11.76 yLineOnOff=on priority=10"
                       :format :variable-step :chr "chr19" :step nil :span 1}
               :chr "chr19" :start 49304901 :end 49304901 :value 12.5}
              {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
                       :format :fixed-step :chr "chr19" :step 1 :span 1}
               :chr "chr19" :start 49307401 :end 49307401 :value 1000}
              {:track {:line "track type=wiggle_0 name=\"fixedStep\" description=\"fixedStep format\" visibility=full autoScale=off viewLimits=0:1000 color=0,200,100 maxHeightPixels=100:50:20 graphType=points priority=20"
                       :format :fixed-step :chr "chr19" :step 1 :span 1}
               :chr "chr19" :start 49307402 :end 49307402 :value 900}])))))

(deftest writer
  (testing "make writer instance"
    (with-before-after {:before (prepare-cache!)
                        :after (clean-cache!)}
      (with-open [wtr (wig/writer (.getAbsolutePath (cio/file temp-dir "temp.wig")))]
        (is (instance? cljam.io.wig.WIGWriter wtr)))))

  (testing "write wig"
    (is (= (-> "variableStep chrom=chr1 span=10\n1 1\n101 2" str->wig wig->str)
           "variableStep chrom=chr1 span=10\n1 1\n101 2"))
    (is (= (-> "fixedStep chrom=chr1 start=1 step=20 span=10\n1\n2" str->wig wig->str)
           "fixedStep chrom=chr1 start=1 step=20 span=10\n1\n2")))

  (testing "write wig with default span value for variableStep"
    (is (= (-> "variableStep chrom=chr1\n1 1\n101 2" str->wig wig->str)
           "variableStep chrom=chr1\n1 1\n101 2")))

  (testing "write wig with default span and step value for fixedStep"
    (is (= (-> "fixedStep chrom=chr1 start=1\n1\n2" str->wig wig->str)
           "fixedStep chrom=chr1 start=1\n1\n2")))

  (testing "write wig with continuous same formats"
    (let [s (cstr/join \newline ["variableStep chrom=chr19 span=5"
                                 "1 1"
                                 "11 10"
                                 "variableStep chrom=chr19 span=10"
                                 "101 1"
                                 "111 10"])]
      (is (= s (-> s str->wig wig->str))))
    (let [s (cstr/join \newline ["fixedStep chrom=chr19 start=1 step=10 span=5"
                                 "1"
                                 "10"
                                 "fixedStep chrom=chr19 start=101 step=10 span=10"
                                 "1"
                                 "10"])]
      (is (= s (-> s str->wig wig->str))))))

(deftest source-type-test
  (testing "reader"
    (with-open [server (http-server)]
      (are [x] (with-open [rdr (wig/reader x)]
                 (= (wig/read-fields rdr) test-wig-fields1))
        test-wig-file1
        (cio/file test-wig-file1)
        (cio/as-url (cio/file test-wig-file1))
        (cio/as-url (str (:uri server) "/wig/test1.wig")))))

  (testing "writer"
    (are [x] (with-before-after {:before (prepare-cache!)
                                 :after (clean-cache!)}
               (with-open [wtr (wig/writer x)]
                 (not-throw? (wig/write-fields wtr test-wig-fields1))))
      temp-wig-file
      (cio/file temp-wig-file)
      (cio/as-url (cio/file temp-wig-file)))))
