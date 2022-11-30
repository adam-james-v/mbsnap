# mbsnap
Save Metabase Cards and Dashboards to png.

This is small script built to help with identifying visual differences between Metabase's App-Viz and Static-Viz.

This is done by interacting with the Metabase app in 2 ways:

1. A headless browser via Etaoin (the first version of this script assumes the Firefox WebDriver is installed)
   - this is used to 'screenshot' Cards and Dashboards.
2. Using either Metabase source directory OR a metabase.jar file as a Clojure library to run the static-viz rendering pipeline on the response from `/api/:card-id/query`.
   - this is used to more quickly see the static-viz result instead of needing to set up a Slack or email alert in the app yourself.

## Usage
Once things are set up, its not too hard to run the main namepsace with `clj -M:run --config config.edn` (for example).

### Setup
First, you'll need to set up a few things.

1. Clone this repo
2. In the repo's root, find `deps.edn`, and edit the `metabase/metabase` coordinate in one of two ways:
   - change the :local/root path to a metabase.jar file you have locally
   - change the :local/root path to the root directory of metabase's source repo
   You'll need to do this since we're using Metabase as a library in this script.
3. Install Firefox's WebDriver eg. `brew install geckodriver`
4. Get your Metabase session cookie value
   - log in to the Metabase Instance you want to take screenshots of
   - In your browser's dev tools, go to 'Network', and inspect any of the Requests to Metabase.
   - Find the 'Cookies' tab, and in the list you should see 'metabase.SESSION'. Copy the value there.
   - optionally, in a terminal, `export MB_COOKIE='your copied value'`. Sanity check with `echo $MB_COOKIE`
5. I recommend setting up a config.edn file that you can pass in.

```clojure
{:width  900 ;; width of rendered image(s) in pixels. Defaults to 1000px if not specified
 :height 700 ;; height of card renders in pixels. Dashboard heights are calculated, so this is ignored. Defaults to create a 16/9 aspect ratio image for card renders
 :domain "http://localhost:3000" ;; the domain, including the Protocol. Required
 :model  :card ;; the model (:card or :dashboard)
 :id     4528 ;; the ID number of the card or dashboard
 :wait   3 ;; Wait time between each browser step (3 steps) for renders. For slower questions/dashboards, higher may be better
 :cookie "your-cookie-can-go-here" ;; metabase.SESSION cookie value. Required for authentication
 }
```

### Usage
Once things are set up, you can run the script with:

`clj -M:run --config your-config.edn --model :card --id 1`

Use `clj -M:run -h` to print a summary of the (currently) available args you can pass.

Any arg passed via the command line supersedes those inside your config. You can use this to configure your cookie and your default width, height, and model, but quickly specify a new --id to use.

At this time, if you set `--model :dashboard`, only the screenshot of the dashboard is taken, no static-viz is run.

## Why A Script like this?
This script was made to meet three goals:
- make it easy to get side-by-side views of the App's visualisation and the static-viz version of the same question
- be able to use a running Metabase instance via the API, because there are real-world visualisations readily available to test with
- make it quicker to file bug reports for visual issues in static-viz. Two screenshots that compare the app and the static result are always generated, making it easy to share them in a github issue.


## Todos
This is really a helper tool, so a bit of unpolished behaviour is likely. However, there are a couple things that might make this more useful:

- [ ] Find a way to not require the user to manually change deps.edn
- [ ] Have a Static-viz render for dashboards, as users can build a 'viz-stress-test' dashboard or something like it
- [ ] make the browser driver configurable (:chrome :safari :firefox)
- [ ] find out how this could be useful for other devs (eg. what do Frontend devs. really want from a tool like this?)
