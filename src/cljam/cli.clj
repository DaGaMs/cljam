(ns cljam.cli
  "Implementations of the command-line tool."
  (:refer-clojure :exclude [sort merge])
  (:require [clojure.string :as str]
            [clj-sub-command.core :refer [sub-command]]
            [clojure.tools.cli :refer [parse-opts]]
            (cljam [core :refer [reader writer]]
                   [sam :as sam]
                   [io :as io]
                   [bam :as bam]
                   [bam-indexer :as bai]
                   [normal :as normal]
                   [sorter :as sorter]
                   [fasta :as fa]
                   [fasta-indexer :as fai]
                   [dict :as dict]
                   [pileup :as plp])
            [cljam.util.sam-util :refer [stringify-header stringify-alignment]]))

;; CLI functions
;; -------------

(defn- exit
  "Exits the program with the status after printing the message."
  [status message]
  (println message)
  (System/exit status))

(defn- error-msg
  "Returns error message strings from the errors."
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn- candidate-msg
  "Returns a message  of candidate sub-commands."
  [candidates]
  (->> candidates
       (map (partial str "        "))
       (cons (if (= (count candidates) 1)
               "Did you mean this?"
               "Did you mean one of these?"))
       (str/join \newline)))

;; Sub-commands
;; ------------

;; ### view command

(def ^:private view-cli-options
  [[nil "--header" "Include header"]
   ["-f" "--format FORMAT" "Input file format <auto|sam|bam>"
    :default "auto"]
   ["-h" "--help" "Print help"]])

(defn- view-usage [options-summary]
  (->> ["Extract/print all or sub alignments in SAM or BAM format."
        ""
        "Usage: cljam view [--header] [-f FORMAT] <in.bam|sam>"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn view [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args view-cli-options)]
    (cond
     (:help options) (exit 0 (view-usage summary))
     (not= (count arguments) 1) (exit 1 (view-usage summary))
     errors (exit 1 (error-msg errors)))
    (let [f (first arguments)]
      (with-open [r (condp = (:format options)
                      "auto" (reader     f)
                      "sam"  (sam/reader f)
                      "bam"  (bam/reader f :ignore-index true))]
        (when (:header options)
          (println (stringify-header (io/read-header r))))
        (doseq [aln (io/read-alignments r {})]
          (println (stringify-alignment aln))))))
  nil)

;; ### convert command

(def ^:private convert-cli-options
  [["-if" "--input-format FORMAT" "Input file format <auto|sam|bam>"
    :default "auto"]
   ["-of" "--output-format FORMAT" "Output file format <auto|sam|bam>"
    :default "auto"]
   ["-h" "--help" "Print help"]])

(defn- convert-usage [options-summary]
  (->> ["Convert SAM to BAM or BAM to SAM."
        ""
        "Usage: cljam convert [-if FORMAT] [-of FORMAT] <in.bam|sam> <out.bam|sam>"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn convert [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args convert-cli-options)]
    (cond
     (:help options) (exit 0 (convert-usage summary))
     (not= (count arguments) 2) (exit 1 (convert-usage summary))
     errors (exit 1 (error-msg errors)))
    (let [[in out] arguments]
      (with-open [wtr (writer out)]
        (with-open [rdr (reader in)]
          (let [hdr (io/read-header rdr)]
            (io/write-header wtr hdr)
            (io/write-refs wtr hdr)
            (doseq [alns (partition-all 10000 (io/read-alignments rdr {}))]
              (io/write-alignments wtr alns hdr)))))))
  nil)

;; ### normalize command

(def ^:private normalize-cli-options
  [["-h" "--help"]])

(defn- normalize-usage [options-summary]
  (->> ["Normalize references of alignments"
        ""
        "Usage: cljam normalize <in.bam|sam> <out.bam|sam>"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn normalize [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args normalize-cli-options)]
    (cond
     (:help options) (exit 0 (normalize-usage summary))
     (not= (count arguments) 2) (exit 1 (normalize-usage summary))
     errors (exit 1 (error-msg errors)))
    (let [[in out] arguments
          r (reader in)
          w (writer out)]
      (normal/normalize r w)))
  nil)

;; ### sort command

(def ^:private sort-cli-options
  [["-o" "--order ORDER" "Sorting order of alignments <coordinate|queryname>"
    :default "coordinate"]
   ["-h" "--help" "Print help"]])

(defn- sort-usage [options-summary]
  (->> ["Sort alignments by leftmost coordinates."
        ""
        "Usage: cljam sort [-o ORDER] <in.bam|sam> <out.bam|sam>"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn sort [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args sort-cli-options)]
    (cond
     (:help options) (exit 0 (sort-usage summary))
     (not= (count arguments) 2) (exit 1 (sort-usage summary))
     errors (exit 1 (error-msg errors)))
    (let [[in out] arguments
          r (reader in)
          w (writer out)]
      (condp = (:order options)
        (name sorter/order-coordinate) (sorter/sort-by-pos r w)
        (name sorter/order-queryname) (sorter/sort-by-qname r w))))
  nil)

;; ### index command

(def ^:private index-cli-options
  [["-h" "--help"]])

(defn- index-usage [options-summary]
  (->> ["Index sorted alignment for fast random access."
        ""
        "Usage: cljam index <in.bam>"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn index [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args index-cli-options)]
    (cond
     (:help options) (exit 0 (index-usage summary))
     (not= (count arguments) 1) (exit 1 (index-usage summary))
     errors (exit 1 (error-msg errors)))
    (let [f (first arguments)]
      (bai/create-index f (str f ".bai"))))
  nil)

;; ### pileup command

(def ^:private pileup-cli-options
  [["-r" "--ref FASTA" "Reference file in the FASTA format."
    :default nil]
   ["-h" "--help"]])

(defn- pileup-usage [options-summary]
  (->> ["Generate pileup for the BAM file."
        ""
        "Usage: cljam pileup [-r FASTA] <in.bam>"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn- pileup-with-ref
  [rdr ref-fa]
  (with-open [fa-rdr (fa/reader ref-fa)]
    (doseq [rname (map :name (io/read-refs rdr))
            line  (plp/mpileup rdr rname -1 -1 :ref-fasta fa-rdr)]
      (if-not (zero? (:count line))
        (println (str/join \tab (map #(% line) [:rname :pos :ref :count :seq :qual])))))))

(defn- pileup-without-ref
  [rdr]
  (doseq [rname (map :name (io/read-refs rdr))
          line  (plp/mpileup rdr rname)]
    (if-not (zero? (:count line))
      (println (str/join \tab (map #(% line) [:rname :pos :ref :count :seq :qual]))))))

(defn pileup [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args pileup-cli-options)]
    (cond
     (:help options) (exit 0 (pileup-usage summary))
     (not= (count arguments) 1) (exit 1 (pileup-usage summary))
     errors (exit 1 (error-msg errors)))
    (let [f (first arguments)]
      (with-open [r (reader f :ignore-index false)]
        (when (= (type r) cljam.sam.reader.SAMReader)
          (exit 1 "Not support SAM file"))
        (when-not (sorter/sorted-by? r)
          (exit 1 "Not sorted"))
        (if (nil? (:ref options))
          (pileup-without-ref r)
          (pileup-with-ref r (:ref options))))))
  nil)

;; ### faidx command

(def ^:private faidx-cli-options
  [["-h" "--help"]])

(defn- faidx-usage [options-summary]
  (->> ["Index reference sequence in the FASTA format."
        ""
        "Usage: cljam faidx <ref.fasta>"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn faidx [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args faidx-cli-options)]
    (cond
     (:help options) (exit 0 (faidx-usage summary))
     (not= (count arguments) 1) (exit 1 (faidx-usage summary))
     errors (exit 1 (error-msg errors)))
    (let [f (first arguments)]
      (fai/create-index f (str f ".fai"))))
  nil)

;; ### dict command

(def ^:private dict-cli-options
  [["-h" "--help"]])

(defn- dict-usage [options-summary]
  (->> ["Create a FASTA sequence dictionary file."
        ""
        "Usage: cljam dict <ref.fasta> <out.dict>"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn dict [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args dict-cli-options)]
    (cond
     (:help options) (exit 0 (dict-usage summary))
     (not= (count arguments) 2) (exit 1 (dict-usage summary))
     errors (exit 1 (error-msg errors)))
    (let [[in out] arguments]
      (dict/create-dict in out)))
  nil)

;; Main command
;; ------------

(defn run [args]
  (let [[opts cmd args help cands]
        (sub-command args
                     "Usage: cljam {view,convert,sort,index,pileup,faidx,dict} ..."
                     :options  [["-h" "--help" "Show help" :default false :flag true]]
                     :commands [["view"    "Extract/print all or sub alignments in SAM or BAM format."]
                                ["convert" "Convert SAM to BAM or BAM to SAM."]
                                ["normalize" "Normalize references of alignments"]
                                ["sort"    "Sort alignments by leftmost coordinates."]
                                ["index"   "Index sorted alignment for fast random access."]
                                ["pileup"  "Generate pileup for the BAM file."]
                                ["faidx"   "Index reference sequence in the FASTA format."]
                                ["dict"    "Create a FASTA sequence dictionary file."]])]
    (when (:help opts)
      (exit 0 help))
    (case cmd
      :view    (view args)
      :convert (convert args)
      :normalize (normalize args)
      :sort    (sort args)
      :index   (index args)
      :pileup  (pileup args)
      :faidx   (faidx args)
      :dict    (dict args)
      (do (println "Invalid command. See 'foo --help'.")
          (when (seq cands)
            (newline)
            (exit 1 (candidate-msg cands)))))))
