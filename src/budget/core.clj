(ns budget.core
  (:require [morse.handlers :as h]
            [morse.api :as t]
            [morse.polling :as p]))

(def tg-token "")
(def allowed-chat 0)

(defn guard [handler msg]
  (let [id (-> msg :chat :id)]
    (if (= id allowed-chat)
      (handler msg)
      (t/send-text tg-token id "401"))))

(defn start-cmd [{{id :id :as chat} :chat}]
  (println "Bot joined a new chat: " chat)
  (t/send-text tg-token id "What's up!"))

(defn help-cmd [{{id :id :as chat} :chat}]
  (t/send-text tg-token id "I'm just a simple bot."))

(defn message-handler [msg]
  (prn "A message" msg)
  #_(t/send-text token id "I didn't get it."))

#_{:clj-kondo/ignore [:unresolved-symbol]}
(h/defhandler bot-api
  (h/command-fn "start" (partial guard start-cmd))
  (h/command-fn "help" (partial guard help-cmd))
  (h/message-fn (partial guard message-handler)))

(defn -main [& _args]
  #_{:clj-kondo/ignore [:unresolved-symbol]}
  (p/start tg-token bot-api))

(comment
  (def channel #_{:clj-kondo/ignore [:unresolved-symbol]}
               (p/start tg-token bot-api))
  (p/stop channel))
