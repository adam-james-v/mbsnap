(ns mbsnap.render-png
  "Improve feedback loop for dealing with png rendering code. Will create images using the rendering that underpins
  pulses and subscriptions and open those images without needing to send them to slack or email."
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [metabase.pulse.render :as render]
            [metabase.shared.models.visualization-settings :as mb.viz]))

(defn- cookie->clj-http-cookie
  [value]
  {"metabase.SESSION" {:value value}})

(defn card-via-api
  [{:keys [domain cookie id]}]
  (-> (http/get (format (str domain "/api/card/%s") id) {:as      :json
                                                         :cookies (cookie->clj-http-cookie cookie)})
      :body
      (update :display keyword)))

(defn- update-column-keys
  [col]
  (let [ks [:semantic_type :unit :source :effective_type :visibility_type :base_type]
        updated (update-vals (select-keys col ks) keyword)]
   (merge col updated)))

(defn query-via-api
  [{:keys [domain cookie id]}]
  (let [query (:body (http/post (format (str domain "/api/card/%s/query") id)
                                {:as :json :cookies (cookie->clj-http-cookie cookie)}))]
    (update-in query [:data :cols] #(map update-column-keys %))))

(defn viz-settings
  [card]
  (-> card :visualization_settings mb.viz/db->norm
      (assoc ::mb.viz/column-settings {})
      (assoc ::mb.viz/global-column-settings {})))

(defn render
  [{:keys [url
           model id
           width
           filename] :as render-request}]
  (when (or (= model :card)
            (str/includes? (or url "") "question"))
    (let [width         (or width 1000)
          filename      (or filename (str (name model) "-" id "-static-viz.png"))
          card          (card-via-api render-request)
          viz           (viz-settings card)
          query-results (-> (query-via-api render-request) (assoc-in [:data :viz-settings] viz))
          png-bytes     (render/render-pulse-card-to-png "UTC" card query-results width)]
      (with-open [w (java.io.FileOutputStream. (java.io.File. filename))]
        (.write w ^bytes png-bytes))
      filename)))
