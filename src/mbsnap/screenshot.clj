(ns mbsnap.screenshot
  (:require [etaoin.api :as e]
            [mbsnap.util :as u]))

(defn size
  [width model-or-height]
  (case model-or-height
    :card [width (int (* width (/ 11 16)))]
    :dashboard [width (min 30000 (* 5 width))]
    [width model-or-height]))

(def model-query
  {:card      {:tag          :div
               :fn/has-class "QueryBuilder"}
   :dashboard {:tag :div
               :css ".spread"}})

(defn- hide-elements-script
  [query]
  (format "
function hide() {
  var divsToHide = document.querySelectorAll('%s');
  for (var i = 0; i < divsToHide.length; i++) {
    divsToHide[i].style.display = 'none';
  }
}
hide()
"
          query))

(defn hide-elements
  [driver query]
  (e/js-execute driver (hide-elements-script query)))

(def model->remove-queries
  {:card
   ["[class*=\"AppBarRoot\"]"
    "[class*=\"ViewHeaderActionPanel\"]"
    "[class*=\"Button\"]"
    "button"
    "[class*=\"ViewHeaderLeftSubHeading\"]"
    "[class*=\"ViewFooterRoot\"]"
    ".flex-no-shrink.text-medium.border-top"
    "main > div.layout-centered"]
   :dashboard
   ["[class*=\"AppBarRoot\"]"
    "main header button"
    "[class*=\"HeaderButtonsContainer\"]"
    "[class*=\"Icon-close\"]"
    "[class*=\"Icon-chevrondown\"]"]})

(defn adjustments
  [driver model]
  (doseq [query (model->remove-queries model)]
    (hide-elements driver query))
  (if (= :card model)
    (e/js-execute driver "document.querySelector('.QueryBuilder').querySelector(':scope > div').style.display='none';")
    (e/js-execute driver "document.querySelector('.spread').querySelector(':scope > header').style.display='none';")))

(defn size-adjustments
  [driver]
  (let [{:keys [width]} (e/get-window-size driver)
        {:keys [y1 y2]} (e/get-element-box driver {:tag :div :fn/has-class "DashboardGrid"})]
    (e/set-window-size driver width (+ y1 y2))))

(defn screenshot
  [{:keys [cookie
           url
           domain model id params
           width height
           filename
           wait]}]
  (let [width    (or width 1000)
        params   (or params {})
        url      (or url (u/build-url domain model id params))
        filename (or filename (str (name model) "-" id "-app-viz.png"))
        wait     (or wait 1)]
    (println width height)
    (e/with-firefox-headless {:size (if-not height
                                      (size width model)
                                      [width height])} driver
      (e/go driver domain)
      (e/set-cookie driver {:name "metabase.SESSION" :value cookie})
      (e/with-wait wait
        (e/go driver url)
        (adjustments driver model)
        (when (= :dashboard model)
          (size-adjustments driver))
        (e/screenshot-element driver (model-query model) filename)))
    filename))
