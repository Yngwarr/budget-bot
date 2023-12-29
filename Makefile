run:
	clojure -M:run
repl:
	clojure -M:repl
auth:
	gcloud auth application-default login --client-id-file=client-id.json --scopes=https://www.googleapis.com/auth/spreadsheets
