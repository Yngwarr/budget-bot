(ns budget.auth
  (:require [clj-http.client :as c])
  (:import com.google.auth.oauth2.GoogleCredentials))

(defn wrap-access-token [client config]
  (let [credential (GoogleCredentials/getApplicationDefault)
        credential (if-let [scopes (:scopes config)]
                     (.createScoped credential scopes)
                     credential)]
    (fn [request]
      (.refreshIfExpired credential)
      (let [token (.getAccessToken credential)
            token (if (not token)
                    (do (.refresh credential)
                        (.getAccessToken credential))
                    token)]
        (client
          (assoc-in
            request
            [:headers "Authorization"]
            (str "Bearer " (.getTokenValue token))))))))

(defn create-client [config]
  (wrap-access-token
    c/request
    config))
