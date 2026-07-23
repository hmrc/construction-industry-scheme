#!/bin/bash
set -uo pipefail

# End-to-end scenario suite for verification flows:
#   backend (this repo, 6994) -> ChRIS/formp external stub (6997)
#
# Drives every stub error scenario twice:
#   - enrolment mode (isAgent=false): the scenario TaxOfficeNumber travels in the
#     HMRC-CIS-ORG enrolment of the bearer token and must be passed down by the
#     backend into the ChRIS XML (the DTR-5655 enrolment pass-down under test)
#   - agent mode (isAgent=true): the scenario TaxOfficeNumber travels in the
#     request body (clientTaxOfficeNumber), the token carries a benign enrolment
#
# Prerequisites (all running locally):
#   - this backend from THIS branch:  sbt run                       (port 6994)
#   - ../construction-industry-scheme-external-stub:  sbt run       (port 6997)
#   - auth-login-api (8585), auth (8500), MongoDB (27017), e.g. sm2 --start CIS_ALL
#     then sm2 --stop CONSTRUCTION_INDUSTRY_SCHEME CONSTRUCTION_INDUSTRY_SCHEME_STUBS
#     so the branch code answers on 6994/6997.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_HOST="${BACKEND_HOST:-http://localhost:6994}"
STUB_HOST="${STUB_HOST:-http://localhost:6997}"
TMP_DIR="$(mktemp -d /tmp/cis-e2e.XXXXXX)"
trap 'rm -rf "$TMP_DIR"' EXIT

PASS=0
FAIL=0
FAILURES=()

red()   { printf '\033[31m%s\033[0m' "$*"; }
green() { printf '\033[32m%s\033[0m' "$*"; }

preflight() {
  local ok=1
  command -v jq >/dev/null || { echo "ERROR: jq is required (brew install jq)"; ok=0; }
  for svc in "backend:$BACKEND_HOST/ping/ping" \
             "stub:$STUB_HOST/ping/ping" \
             "auth:http://localhost:8500/ping/ping" \
             "auth-login-api:http://localhost:8585/ping/ping"; do
    local name="${svc%%:*}" url="${svc#*:}"
    local code
    code=$(curl -s -o /dev/null -m 60 -w "%{http_code}" "$url" || true)
    if [[ "$code" != "200" ]]; then
      echo "ERROR: $name not reachable at $url (got '$code')"
      ok=0
    fi
  done
  if [[ "$ok" -ne 1 ]]; then
    cat >&2 <<'EOF'

Start the missing services first, e.g.:
  sm2 --start CIS_ALL
  sm2 --stop CONSTRUCTION_INDUSTRY_SCHEME     # then run this branch: sbt run
  (cd ../construction-industry-scheme-external-stub && sbt run)   # stub on 6997
EOF
    exit 1
  fi
}

# run_scenario <mode> <ton> <tor> <label> <expect_submit_http> <expect_submit_status> <expect_poll_status> [expect_poll_error_number] [poll_url_override]
#   mode: enrolment | agent | no-enrolment
#   expect_poll_status: "-" = do not poll
#   expect_poll_error_number: "-" = do not check error.errorNumber of the poll body
#   poll_url_override: "-" = poll the responseEndPoint.url from the submit;
#     otherwise poll this URL instead (still after a successful submit, so the
#     GovTalk record for the submissionId exists)
run_scenario() {
  local mode="$1" ton="$2" tor="$3" label="$4"
  local exp_http="$5" exp_status="$6" exp_poll="$7" exp_poll_err="${8:--}" poll_override="${9:--}"

  local sid="e2e-$mode-$ton-$(date +%s)-$RANDOM"
  local body_file="$TMP_DIR/$sid-submit.json"
  local call_args=(-c -o "$body_file" -s "$sid" -t "$ton" -r "$tor")
  case "$mode" in
    agent)        call_args+=(-a) ;;
    no-enrolment) call_args+=(-n) ;;
  esac

  local http status result="PASS" detail=""
  http=$("$SCRIPT_DIR/call-backend.sh" "${call_args[@]}" 2>/dev/null)
  status=$(jq -r '.status // empty' "$body_file" 2>/dev/null)

  [[ "$http" == "$exp_http" ]] || { result="FAIL"; detail+="submit http=$http (want $exp_http) "; }
  if [[ "$exp_status" != "ANY" && "$status" != "$exp_status" ]]; then
    result="FAIL"; detail+="submit status=${status:-<none>} (want $exp_status) "
  fi

  local poll_status=""
  if [[ "$exp_poll" != "-" && "$result" == "PASS" ]]; then
    local poll_url
    if [[ "$poll_override" != "-" ]]; then
      poll_url="$poll_override"
    else
      poll_url=$(jq -r '.responseEndPoint.url // empty' "$body_file")
    fi
    if [[ -z "$poll_url" ]]; then
      result="FAIL"; detail+="no responseEndPoint.url to poll "
    else
      local poll_body="$TMP_DIR/$sid-poll.json"
      local poll_args=(-c -o "$poll_body" -s "$sid" -p "$poll_url" -t "$ton" -r "$tor")
      case "$mode" in
        agent)        poll_args+=(-a) ;;
        no-enrolment) poll_args+=(-n) ;;
      esac
      local poll_http
      poll_http=$("$SCRIPT_DIR/call-backend.sh" "${poll_args[@]}" 2>/dev/null)
      poll_status=$(jq -r '.status // empty' "$poll_body" 2>/dev/null)
      if [[ "$poll_http" != "200" ]]; then
        result="FAIL"; detail+="poll http=$poll_http (want 200) "
      elif [[ "$poll_status" != "$exp_poll" ]]; then
        result="FAIL"; detail+="poll status=${poll_status:-<none>} (want $exp_poll) "
      elif [[ "$exp_poll_err" != "-" ]]; then
        local poll_err
        poll_err=$(jq -r '.error.errorNumber // empty' "$poll_body" 2>/dev/null)
        if [[ "$poll_err" != "$exp_poll_err" ]]; then
          result="FAIL"; detail+="poll error.errorNumber=${poll_err:-<none>} (want $exp_poll_err) "
        fi
      fi
    fi
  fi

  local shown="$status"
  [[ -n "$poll_status" ]] && shown="$status -> $poll_status"
  if [[ "$result" == "PASS" ]]; then
    printf '  %s  [%-12s] TON=%-4s %-58s http=%s status=%s\n' \
      "$(green PASS)" "$mode" "$ton" "$label" "$http" "$shown"
    PASS=$((PASS + 1))
  else
    printf '  %s  [%-12s] TON=%-4s %-58s %s\n' \
      "$(red FAIL)" "$mode" "$ton" "$label" "$detail"
    FAILURES+=("[$mode] TON=$ton $label: $detail")
    FAIL=$((FAIL + 1))
  fi
}

# Scenario tables
#   fields: TON | TOR | label | expected submit HTTP | expected submit status
#           | expected poll status ("-" = no poll)
#           | expected poll error.errorNumber (optional, omit or "-" = don't check)
#           | poll URL override (optional, omit or "-" = poll the ack's URL)
#
# The TaxOfficeNumber reaches the stub through several legs, which dispatch on
# different sources, so expectations differ per mode. Contract as of stub main
# @ da1a303 (#107, regime-aware dispatch) and backend DTR-5655 merge:
#   - ChRIS submit (XML): TON from enrolment (non-agent) or body (agent);
#     500-505 -> that HTTP status on submit.
#   - IR-CIS-VERIFY has its OWN scenario knobs now (the old 754-761 values only
#     drive the CIS300MR monthly-return regime):
#       verify fatalErrorFilter: 779/EZ00125 -> immediate FATAL_ERROR
#       verify polling statusMap: 774=SUBMITTED 775=FATAL_ERROR
#         776=DEPARTMENTAL_ERROR (3001/business) 777=SUBMITTED_NO_RECEIPT
#         778=ACKNOWLEDGE (forever pending; poll falls through to success)
#     No recoverable (3000/2005/1000) entries exist for VERIFY at all.
#   - /cis-taxpayer: "500" as the employer-ref TON -> HTTP 500 on the ChRIS
#     failure leg. The verification flow derives the employer ref from the BODY
#     unconditionally (SubmissionController handleSubmitVerificationToChris; the
#     enrolment-derived resolveEmployerRef is only used by monthly returns), so
#     this magic fires in AGENT mode only: agent-500 -> backend 500, but
#     enrolment-500 -> backend 200 FATAL_ERROR like any other ChRIS 5xx.
#     If that asymmetry is fixed to use enrolments for non-agents (DTR-5655),
#     the enrolment-500 row below must flip to 500/FATAL_ERROR/-.
#   - /cis/govtalkstatus/*: failures are now driven by a ?scenario=500|502|404
#     query param (stub #107), which the backend never sends - the old
#     enrolment-based govtalk failure leg is no longer reachable end-to-end.
#   - 780: F18 scenario 5 - error 3000 + type "fatal" on the verify poll maps
#     to DEPARTMENTAL_ERROR (ChrisVerificationPollXmlMapper), distinguished
#     from scenario 6 (776: 3001/business) by error.errorNumber. Needs a stub
#     with the 780 -> DEPARTMENTAL_ERROR_3000 statusMap entry and the
#     submitCISVerifyMessage-departmentalError-3000-response.xml resource
#     (added 2026-07-13); against an older stub these rows fail with
#     poll status=SUBMITTED.

# "F18 s<n>:" prefixes = the tester's F18/DTR-5655 scenario numbers (s1
# request>5xx, s2 request>GovTalk error, s3 poll>5xx, s4 poll>fatal,
# s5 poll>3000/fatal, s6 poll>3001/business, s7 poll>other error number -
# s7 hits the same mapper branch as s4, exercised by the 775 row via err 1001).
S3_POLL_URL="$STUB_HOST/submission/ChRIS/poll/IR-CIS-VERIFY/2?final=SERVER_ERROR_500"

ENROLMENT_SCENARIOS=(
  "123|EZ00100|happy path (success on poll)|202|ACCEPTED|SUBMITTED"
  "500|EZ00100|F18 s1: ChRIS HTTP 500 on submit (taxpayer benign: body ref=123)|200|FATAL_ERROR|-"
  "502|EZ00100|F18 s1: ChRIS HTTP 502 on submit|200|FATAL_ERROR|-"
  "503|EZ00100|F18 s1: ChRIS HTTP 503 on submit|200|FATAL_ERROR|-"
  "779|EZ00125|F18 s2: immediate FATAL_ERROR from ChRIS|200|FATAL_ERROR|-"
  "123|EZ00100|F18 s3: ChRIS HTTP 500 on poll (crafted count>=2 URL)|202|ACCEPTED|ACCEPTED|500|$S3_POLL_URL"
  "775|EZ00100|F18 s4+s7: FATAL_ERROR on poll (err 1001 = other)|202|ACCEPTED|FATAL_ERROR|1001"
  "780|EZ00100|F18 s5: DEPARTMENTAL_ERROR 3000/fatal on poll|202|ACCEPTED|DEPARTMENTAL_ERROR|3000"
  "776|EZ00100|F18 s6: DEPARTMENTAL_ERROR 3001/business on poll|202|ACCEPTED|DEPARTMENTAL_ERROR|3001"
  "777|EZ00100|SUBMITTED_NO_RECEIPT on poll|202|ACCEPTED|SUBMITTED_NO_RECEIPT"
  "778|EZ00100|forever-pending ack (poll succeeds)|202|ACCEPTED|SUBMITTED"
)

AGENT_SCENARIOS=(
  "123|EZ00100|happy path (success on poll)|202|ACCEPTED|SUBMITTED"
  "500|EZ00100|F18 s1: ChRIS 500 + taxpayer 500 on failure leg|500|FATAL_ERROR|-"
  "502|EZ00100|F18 s1: ChRIS HTTP 502 on submit|200|FATAL_ERROR|-"
  "503|EZ00100|F18 s1: ChRIS HTTP 503 on submit|200|FATAL_ERROR|-"
  "779|EZ00125|F18 s2: immediate FATAL_ERROR from ChRIS|200|FATAL_ERROR|-"
  "123|EZ00100|F18 s3: ChRIS HTTP 500 on poll (crafted count>=2 URL)|202|ACCEPTED|ACCEPTED|500|$S3_POLL_URL"
  "775|EZ00100|F18 s4+s7: FATAL_ERROR on poll (err 1001 = other)|202|ACCEPTED|FATAL_ERROR|1001"
  "780|EZ00100|F18 s5: DEPARTMENTAL_ERROR 3000/fatal on poll|202|ACCEPTED|DEPARTMENTAL_ERROR|3000"
  "776|EZ00100|F18 s6: DEPARTMENTAL_ERROR 3001/business on poll|202|ACCEPTED|DEPARTMENTAL_ERROR|3001"
  "777|EZ00100|SUBMITTED_NO_RECEIPT on poll|202|ACCEPTED|SUBMITTED_NO_RECEIPT"
  "778|EZ00100|forever-pending ack (poll succeeds)|202|ACCEPTED|SUBMITTED"
)

preflight
echo
echo "=== e2e scenarios (backend $BACKEND_HOST -> stub $STUB_HOST) ==="

for mode in enrolment agent; do
  echo
  echo "--- $mode mode ---"
  if [[ "$mode" == "enrolment" ]]; then
    rows=("${ENROLMENT_SCENARIOS[@]}")
  else
    rows=("${AGENT_SCENARIOS[@]}")
  fi
  for row in "${rows[@]}"; do
    IFS='|' read -r ton tor label exp_http exp_status exp_poll exp_poll_err poll_override <<<"$row"
    run_scenario "$mode" "$ton" "$tor" "$label" "$exp_http" "$exp_status" "$exp_poll" "$exp_poll_err" "$poll_override"
  done
done

echo
echo "--- negative ---"
run_scenario "no-enrolment" "123" "EZ00100" "isAgent=false without HMRC-CIS-ORG -> 500" "500" "ANY" "-"

echo
echo "=== Summary: $PASS passed, $FAIL failed ==="
if [[ "$FAIL" -gt 0 ]]; then
  for f in "${FAILURES[@]}"; do echo "  - $f"; done
  exit 1
fi
