(require '[clojure.tools.logging]
         '[clojure.java.io])

;; TODO: better way to silence WARNINGS that are not directly relevant to the script?
(defn- disable-logging
  "Run this to disable logging from printing to the terminal."
  []
  (alter-var-root
   #'clojure.tools.logging/*logger-factory*
   (fn [_] clojure.tools.logging.impl/disabled-logger-factory)))

(disable-logging)

(binding [*out* (clojure.java.io/writer (clojure.java.io/output-stream "/dev/null"))
          *err* (clojure.java.io/writer (clojure.java.io/output-stream "/dev/null"))]
  (ns mbsnap.main
    (:require [clojure.edn :as edn]
              [clojure.string :as str]
              [clojure.tools.cli :as cli]
              [mbsnap.render-png :as static]
              [mbsnap.screenshot :as app]
              [mbsnap.util :as u])))

(defn try-slurp
  [s]
  (try (slurp s)
       (catch Throwable ex
         nil
         s)))

(def cli-options
  [["-h" "--help"]
   ["-c" "--config CONFIG" "Configuration as .edn file or string."
    :parse-fn (fn [s] (-> s try-slurp edn/read-string))
    :validate [map? "Config must be a valid Clojure map."]]
   ["-k" "--cookie COOKIE" "metabase.SESSION cookie value"
    :parse-fn (fn [s] {:name "metabase.SESSION" :value s})]
   ["-d" "--domain DOMAIN" "Domain, including the protocol."
    :parse-fn (fn [s] (if (str/ends-with? s "/") (apply str (drop-last s)) s))
    :validate [(fn [s] (str/includes? s "://")) "Must include the Protocol. Eg. 'https://'"]]
   ["-m" "--model MODEL" "Model"
    :parse-fn keyword
    :validate [#{:card :dashboard} "Must be \"card\" or \"dashboard\"."]]
   ["-i" "--id ID" "Model ID"
    :parse-fn #(Integer/parseInt %)
    :validate [pos-int? "Must be a positive Integer"]]
   ["-x" "--width WIDTH" "Width in pixels of the rendered image(s)."
    :parse-fn #(Integer/parseInt %)
    :validate [pos-int? "Must be a positive Integer"]]
   ["-y" "--height HEIGHT" "Height in pixels of the rendered images. Only Applies to Card screenshots."
    :parse-fn #(Integer/parseInt %)
    :validate [pos-int? "Must be a positive Integer"]]
   ["-t" "--wait WAIT" "Wait Time in seconds for page loads for the App Screenshot."
    :parse-fn #(Integer/parseInt %)
    :validate [pos-int? "Must be a positive Integer"]]
   ["-f" "--save-dir SAVE-DIR" "Directory where screenshots will be saved."]])

;; TODO: implement check if driver is installed
(defn check-browser-driver-installed
  []
  true)

;; TODO: does pcalls make sense? Is this worth it?
(defn- render-images
  [{:keys [model] :as config}]
  (pcalls
    [(when (= :card model)
       (do (-> (static/render config) u/open)
           (println "Opening static-viz render.")))
     (do (-> (app/screenshot config) u/open)
         (println "Opening app-viz render."))]))

(defn -main
  [& args]
  (when (check-browser-driver-installed)
    (let [{:keys [options summary]} (cli/parse-opts args cli-options)
          {:keys [config help]}     options]
      #_(println (merge config (dissoc options :config)))
      (println "Rendering. The Browser Screenshot may take a couple seconds.")
      (cond
        help   (println summary)
        config (render-images (merge config (dissoc options :config)))
        :else  (render-images options))
      (System/exit 0))))
