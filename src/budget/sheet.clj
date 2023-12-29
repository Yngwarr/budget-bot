(ns budget.sheet
  (:require
   [budget.auth :as auth]
   [clojure.edn :as edn]
   [jsonista.core :as json]))

(def endpoint  "https://sheets.googleapis.com/v4/spreadsheets/")

(def request
  (auth/create-client {:scopes ["https://www.googleapis.com/auth/spreadsheets"]}))

(defn crawl-range [sheet-id range]
  (let [response
        (request {:method :get
                  :url (str endpoint sheet-id "/values/" range)})]
    (assoc response :body (json/read-value (:body response)))))

(defn add-info [sheet-id [date amount category comment]]
  (let [range "2023!A1:D1000"
        body (json/write-value-as-string
               {"range" range
                "majorDimension" "ROWS"
                "values" [[date amount category comment]]})]
    (request {:method :post
              :url (str endpoint sheet-id "/values/" range ":append")
              :query-params {"valueInputOption" "USER_ENTERED"
                             "insertDataOption" "INSERT_ROWS"
                             "includeValuesInResponse" "false"
                             "responseValueRenderOption" "FORMATTED_VALUE"}
              :body body})))

(comment
  (def sheet (-> (slurp "secret.edn") edn/read-string :sheet))

  (try
    (add-info sheet "2023-12-29" "9.12" "test" "Testing")
    (catch Exception e (-> e ex-data :body println)))

  (def res
    (try
      (crawl-range sheet "2023!A1:A1000")
      (catch Exception e (-> e ex-data :body println))))

  (-> (get-in res [:body "values"]) count)
  (crawl-range sheet "2023!A376")

  (def spreadsheet
    {:sheets
     [{:properties {:sheetId 973124427,
                    :title "Dashboard",
                    :index 0,
                    :sheetType "GRID",
                    :gridProperties {:rowCount 1000,
                                     :columnCount 26}}}
      {:properties {:sheetId 0,
                    :title "2023",
                    :index 1,
                    :sheetType "GRID",
                    :gridProperties {:rowCount 1014,
                                     :columnCount 26,
                                     :frozenRowCount 1}}}
      {:properties {:sheetId 1603891517,
                    :title "Monthly 2023",
                    :index 2,
                    :sheetType "GRID",
                    :gridProperties {:rowCount 1001,
                                     :columnCount 26}}}],
     :spreadsheetUrl "https://docs.google.com/spreadsheets/d/1xlAomgHNiEGf7xTuDlnCEXbccNaLMh9vSy4YOvtaq1Y/edit"})
  )
