(ns budget.parse-bank
  (:require [clojure.string :as str]))

;; TODO move to data somewhere in the spreadsheet
(defn guess-category-and-comment [description]
  (cond
    (str/includes? description "Kindle Svcs")
    ["virtual" "Kindle book"]

    (str/includes? description "Nintendo")
    ["virtual" "Switch game"]

    (str/includes? description "AROMA 127")
    ["food" "City Market"]

    (str/includes? description "AROMA")
    ["food" "Aroma"]

    (str/includes? description "IDEA")
    ["food" "Idea"]

    (str/includes? description "OKOV")
    ["housewares" "Okov"]

    (str/includes? description "GOODFELLAS")
    ["food-outside" "GoodFellas"]

    (str/includes? description "TESLA")
    ["travel" "Tesla Taxi"]

    (str/includes? description "Patreon")
    ["virtual" "Patreon membership"]

    :else
    ["uncategorized" description]))

(defn parse-amount [amount]
  (try
    ;; we don't know the amount of digits in the whole part of the number, but
    ;; after the floating point, there are always two digits of cents. we use
    ;; this knowledge to avoid regular expressions
    (let [dot-index (str/index-of amount ".")]
      (if (nil? dot-index)
        {:error (str "Couldn't parse the amount, got '" amount "'.")}
        {:success (Float/parseFloat (subs amount 0 (+ 3 dot-index)))}))
    (catch NumberFormatException _e
      {:error (str "Couldn't convert the amount to float, got '" amount "'.")})))

(defn parse-date [date-input]
  (let [space-idx (str/index-of date-input " ")]
    (if (nil? space-idx)
      {:error "Can't parse date, expected a space, got '" date-input "'."})
    (let [date (as-> (subs date-input 0 space-idx) $
                 (str/split $ #"\.")
                 (reverse $)
                 (str/join "-" $))]
      (if (some? (re-matches #"\d{4}-\d{2}-\d{2}" date))
        {:success date}
        {:error "Something's wrong with the date, got '" date "'."}))))

(defn parse-values [text]
  (let [splitted (->> text
                      str/split-lines
                      (mapv #(str/split % #": " 2)))
        mappable? (reduce (fn [acc value]
                            (if (= 2 (count value))
                              (and acc true)
                              (reduced false)))
                          true splitted)
        ]
    (if mappable?
      {:success (into {} splitted)}
      {:error "Can't split the message correctly."})))

(defn parse-msg [text]
  (let [values (parse-values text)]
    (if (:error values)
      values
      (let [values (:success values)
            status (get values "Status")]
        (if (not= status "ODOBRENO")
          {:error "The payment didn't go through (status != ODOBRENO)."}
          (let [amount (parse-amount (get values "Iznos"))
                date (parse-date (get values "Vrijeme"))
                [category comment] (guess-category-and-comment (get values "Opis"))]
            (cond
              (:error amount) amount
              (:error date) date
              :else {:success [(:success date) (:success amount) category comment]})))))))

(comment
  (def sample "Kartica: 8718\nIznos: 9.03 EUR\nVrijeme: 29.12.2023 04:39:58\nStatus: ODOBRENO\nOpis: Kindle Svcs            888-802-3080   US\nRaspolozivo: 521.49 EUR")
  (println sample)
  (parse-msg sample)

  (def sample-amount "9.12 EUR")
  (Float/parseFloat "9,12 EUR")
  (parse-amount "9.812 EUR")

  )
