(ns budget.core
  (:require
   [budget.parse-bank :as bank]
   [budget.sheet :as sheet]
   [clojure.edn :as edn]
   [morse.api :as t]
   [morse.handlers :as h]
   [morse.polling :as p]))

(def secret (edn/read-string (slurp "secret.edn")))

(def tg-token (:tg-token secret))
(def allowed-chats (:allowed-chats secret))
(def sheet (:sheet secret))

(defn guard [handler msg]
  (let [id (-> msg :chat :id)]
    (if (some? (get allowed-chats id))
      (handler msg)
      (t/send-text tg-token id "401"))))

(defn start-cmd [{{id :id :as chat} :chat}]
  (println "Bot joined a new chat: " chat)
  (t/send-text tg-token id "What's up!"))

(defn help-cmd [{{id :id :as _chat} :chat}]
  (t/send-text tg-token id "I'm just a simple bot."))

(defn message-handler [msg]
  (prn "Incoming" msg)
  (let [id (get-in msg [:chat :id])
        text (:text msg)
        data (bank/parse-msg text)]
    (cond
      (:error data)
      (t/send-text
        tg-token
        id
        (str "Something went horribly wrong. " (:error data)))

      :else
      (let [data (:success data)
            response (sheet/add-info sheet data)]
        (if (= 200 (:status response))
          (t/send-text
            tg-token
            id
            "Info added, thank you!")
          (let [status (:status response)
                reason (:reason-phrase response)]
            (.println
              *err*
              (format "SHEETS: %d %s, body: %s" status reason (:body response)))
            (t/send-text
              tg-token
              id
              (format "Google doesn't want us. Responded with %d %s."
                      (:status response)
                      (:reason-phrase response)))))))))

#_{:clj-kondo/ignore [:unresolved-symbol]}
(h/defhandler bot-api
  (h/command-fn "start" (partial guard start-cmd))
  (h/command-fn "help" (partial guard help-cmd))
  (h/message-fn (partial guard message-handler)))

(defn -main [& _args]
  #_{:clj-kondo/ignore [:unresolved-symbol]}
  (p/start tg-token bot-api))

(comment
  (def channel
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (p/start tg-token bot-api))
  (p/stop channel)
  )
