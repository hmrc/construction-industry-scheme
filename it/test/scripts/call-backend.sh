#!/bin/bash
set -euo pipefail

# Calls this backend's verification endpoints end-to-end (backend -> ChRIS/formp
# stub) with automatic auth token retrieval. Counterpart of the stub repo's
# call-stub.sh, but exercising the real backend on port 6994.
#
# Prerequisites (all running locally):
#   - this backend from THIS branch:  sbt run                        (port 6994)
#   - the external stub:              ../construction-industry-scheme-external-stub  sbt run  (port 6997)
#   - auth-login-api (8585), auth (8500), MongoDB (27017) - e.g. via sm2
#   NOTE: if sm2 also manages CONSTRUCTION_INDUSTRY_SCHEME / the stub, stop
#   those first so the branch code is what answers on 6994/6997.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_HOST="${BACKEND_HOST:-http://localhost:6994}"
# app.Routes is mounted under /cis in prod.routes
ROUTE_PREFIX="/cis"

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Submit a CIS verification to ChRIS via the backend, or poll one.

Options:
  -t <number>   Tax office number driving the stub scenario (default: 123)
                500-505 -> ChRIS answers that HTTP status on submit
                754 (+ -r EZ00125) -> immediate FATAL_ERROR from ChRIS
                755/756/757 -> FATAL_ERROR / DEPARTMENTAL_ERROR / SUBMITTED_NO_RECEIPT on poll
                759/760/761 -> recoverable errors 3000/2005/1000 on poll
  -r <ref>      Tax office reference (default: EZ00100)
  -a            Agent mode: isAgent=true, -t/-r go into the request body
                (clientTaxOfficeNumber/Ref); the bearer token gets a benign
                123/EZ00100 enrolment. Without -a, -t/-r go into the
                HMRC-CIS-ORG enrolment of the token (the DTR-5655 pass-down).
  -n            Mint the token WITHOUT the HMRC-CIS-ORG enrolment (negative test)
  -s <id>       submissionId (default: e2e-<epoch>)
  -e <path>     Explicit endpoint path (default: /submissions/<submissionId>/submit-verification-to-chris)
  -p <pollUrl>  Poll mode: GET /submissions/verification/poll for <pollUrl>
                (use the responseEndPoint.url returned by a 202 submit)
  -b <json>     Custom JSON request body (overrides the default)
  -X <method>   HTTP method override (default POST, or GET in poll mode)
  -o <file>     Write response body to <file> (default: /tmp/backend-response-body.txt)
  -c            Print only the HTTP status code on stdout (body still goes to -o file)
  -h            Show this help message

Examples:
  $(basename "$0")                       # happy path: 202 ACCEPTED + poll URL
  $(basename "$0") -t 502                # ChRIS 502 via the enrolment
  $(basename "$0") -a -t 502             # same scenario via the agent/body path
  $(basename "$0") -n                    # no enrolment -> backend 500
  $(basename "$0") -p 'http://localhost:6997/submission/ChRIS/poll/IR-CIS-VERIFY/0?final=FATAL_ERROR' -s e2e-123
EOF
  exit 0
}

TAX_OFFICE_NUMBER="123"
TAX_OFFICE_REFERENCE="EZ00100"
AGENT_MODE=0
NO_ENROLMENT=0
SUBMISSION_ID="e2e-$(date +%s)"
ENDPOINT=""
POLL_URL=""
BODY=""
METHOD=""
OUT_FILE="/tmp/backend-response-body.txt"
CODE_ONLY=0

while getopts "t:r:ans:e:p:b:X:o:ch" opt; do
  case "$opt" in
    t) TAX_OFFICE_NUMBER="$OPTARG" ;;
    r) TAX_OFFICE_REFERENCE="$OPTARG" ;;
    a) AGENT_MODE=1 ;;
    n) NO_ENROLMENT=1 ;;
    s) SUBMISSION_ID="$OPTARG" ;;
    e) ENDPOINT="$OPTARG" ;;
    p) POLL_URL="$OPTARG" ;;
    b) BODY="$OPTARG" ;;
    X) METHOD="$OPTARG" ;;
    o) OUT_FILE="$OPTARG" ;;
    c) CODE_ONLY=1 ;;
    h) usage ;;
    *) usage ;;
  esac
done

log() { if [[ "$CODE_ONLY" -eq 0 ]]; then echo "$@" >&2; fi }

# -t/-r drive the scenario: via the token enrolment (non-agent) or the body (agent)
if [[ "$AGENT_MODE" -eq 1 ]]; then
  IS_AGENT="true"
  TOKEN_TON="123"
  TOKEN_TOR="EZ00100"
  BODY_TON="$TAX_OFFICE_NUMBER"
  BODY_TOR="$TAX_OFFICE_REFERENCE"
else
  IS_AGENT="false"
  TOKEN_TON="$TAX_OFFICE_NUMBER"
  TOKEN_TOR="$TAX_OFFICE_REFERENCE"
  BODY_TON="123"
  BODY_TOR="EZ00100"
fi

TOKEN_ARGS=()
if [[ "$NO_ENROLMENT" -eq 1 ]]; then
  TOKEN_ARGS+=("-n")
  log "--- Obtaining bearer token (NO HMRC-CIS-ORG enrolment) ---"
else
  log "--- Obtaining bearer token (enrolment TaxOfficeNumber=$TOKEN_TON, TaxOfficeReference=$TOKEN_TOR) ---"
fi
TOKEN=$("$SCRIPT_DIR/bearer-token.sh" ${TOKEN_ARGS[@]+"${TOKEN_ARGS[@]}"} "$TOKEN_TON" "$TOKEN_TOR")
log "--- Token obtained ---"

# VerificationSubmissionContextBuilder joins verifications to subcontractors by
# verificationResourceRef == subbieResourceRef (as string); mismatched refs -> 400.
# The subcontractor identity (names/tradingName/utr) must match the hardcoded
# subcontractor in the stub's submitCISVerifyMessage-success-response.xml, or
# VerificationResultMapper fails the poll with "No matching requested
# verification" -> backend 500.
default_body() {
  cat <<JSON
{
  "instanceId": "e2e-instance-$SUBMISSION_ID",
  "isAgent": $IS_AGENT,
  "clientTaxOfficeNumber": "$BODY_TON",
  "clientTaxOfficeRef": "$BODY_TOR",
  "contractorUTR": "1234567890",
  "contractorAORef": "123PP87654321",
  "verificationBatchId": "batch-$SUBMISSION_ID",
  "verificationBatchResourceRef": "77",
  "emailRecipient": "test@test.com",
  "subcontractors": [
    {
      "subcontractorId": 1,
      "subbieResourceRef": 10,
      "firstName": "Noel",
      "surname": "Armstrong",
      "tradingName": "DBB Construction",
      "utr": "8786438047",
      "nino": "AB623456C",
      "subcontractorType": "soletrader",
      "addressLine1": "1 Test Street",
      "postcode": "NE1 1AA",
      "worksReferenceNumber": "WRN123"
    }
  ],
  "verifications": [
    {
      "subcontractorName": "Noel Armstrong",
      "verificationResourceRef": "10",
      "proceedVerification": true
    }
  ]
}
JSON
}

CURL_ARGS=(
  -s -o "$OUT_FILE" -w "%{http_code}"
  -H "Authorization: $TOKEN"
  -H "X-Session-ID: session-$(date +%s)"
)

if [[ -n "$POLL_URL" ]]; then
  URL="${BACKEND_HOST}${ROUTE_PREFIX}/submissions/verification/poll"
  METHOD="${METHOD:-GET}"
  CURL_ARGS+=(-X "$METHOD" -G
    --data-urlencode "pollUrl=$POLL_URL"
    --data-urlencode "submissionId=$SUBMISSION_ID")
  log ""
  log "--- Polling $URL (pollUrl=$POLL_URL, submissionId=$SUBMISSION_ID) ---"
else
  ENDPOINT="${ENDPOINT:-$ROUTE_PREFIX/submissions/$SUBMISSION_ID/submit-verification-to-chris}"
  URL="${BACKEND_HOST}${ENDPOINT}"
  METHOD="${METHOD:-POST}"
  if [[ -z "$BODY" ]]; then
    BODY=$(default_body)
  fi
  CURL_ARGS+=(-X "$METHOD" -H "Content-Type: application/json" -d "$BODY")
  log ""
  log "--- Calling $METHOD $URL (isAgent=$IS_AGENT, scenario TaxOfficeNumber=$TAX_OFFICE_NUMBER) ---"
fi

HTTP_CODE=$(curl "${CURL_ARGS[@]}" "$URL")

log "HTTP Status: $HTTP_CODE"

if [[ "$CODE_ONLY" -eq 1 ]]; then
  echo "$HTTP_CODE"
else
  cat "$OUT_FILE"
  echo
fi
