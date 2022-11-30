(ns mbsnap.render-png
  "Improve feedback loop for dealing with png rendering code. Will create images using the rendering that underpins
  pulses and subscriptions and open those images without needing to send them to slack or email."
  (:require [clj-http.client :as http]
            [clojure.string :as str])
  (:import java.util.Base64))

(defn decode [to-decode]
  (.decode (Base64/getDecoder) to-decode))

(defn- cookie->clj-http-cookie
  [value]
  {"metabase.SESSION" {:value value}})

(defn png-bytes-via-api
  [{:keys [domain cookie id]}]
  (-> (http/get (format (str domain "/api/pulse/preview_card/%s") id)
                {:cookies (cookie->clj-http-cookie cookie)})
      :body
      (->> (re-seq #"src=\".*?\"") last)
      (str/replace #"src=\"data:image/png;base64," "")
      (str/replace #"\"" "")
      decode))

(defn render
  [{:keys [url
           model id
           save-dir] :as render-request}]
  (when (or (= model :card)
            (str/includes? (or url "") "question"))
    (let [filename          (str (name model) "-" id "-static-viz.png")
          full-filename     (if save-dir
                              (str save-dir (when-not (str/ends-with? save-dir "/") "/") filename)
                              filename)
          png-bytes         (png-bytes-via-api render-request)]
      (with-open [w (java.io.FileOutputStream. (java.io.File. full-filename))]
            (.write w ^bytes png-bytes))
      full-filename)))
