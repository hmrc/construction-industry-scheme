#!/bin/bash
set -uo pipefail

# End-to-end scenario suite for the DTR-5655 verification flow:
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
    code=$(curl -s -o /dev/null -m 3 -w "%{http_code}" "$url" || true)
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

# run_scenario <mode> <ton> <tor> <label> <expect_submit_http> <expect_submit_status> <expect_poll_status>
#   mode: enrolment | agent | no-enrolment
#   expect_poll_status: "-" = do not poll
run_scenario() {
  local mode="$1" ton="$2" tor="$3" label="$4"
  local exp_http="$5" exp_status="$6" exp_poll="$7"

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
    poll_url=$(jq -r '.responseEndPoint.url // empty' "$body_file")
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
      fi
    fi
  fi

  local shown="$status"
  [[ -n "$poll_status" ]] && shown="$status -> $poll_status"
  if [[ "$result" == "PASS" ]]; then
    printf '  %s  [%-12s] TON=%-4s %-42s http=%s status=%s\n' \
      "$(green PASS)" "$mode" "$ton" "$label" "$http" "$shown"
    PASS=$((PASS + 1))
  else
    printf '  %s  [%-12s] TON=%-4s %-42s %s\n' \
      "$(red FAIL)" "$mode" "$ton" "$label" "$detail"
    FAILURES+=("[$mode] TON=$ton $label: $detail")
    FAIL=$((FAIL + 1))
  fi
}

# Scenario tables
#   fields: TON | TOR | label | expected submit HTTP | expected submit status | expected poll status ("-" = no poll)
#
# The TaxOfficeNumber reaches the stub through several legs, which dispatch on
# different sources, so expectations differ per mode:
#   - ChRIS submit (XML): TON from enrolment (non-agent) or body (agent); 500-505 -> that HTTP status
#   - /cis-taxpayer (getCisTaxpayer): TON from the resolved employer reference
#     (enrolment or body); "500" -> HTTP 500. Runs while handling a ChRIS failure,
#     so scenario 500 always ends as backend HTTP 500 (recover path).
#   - /cis/govtalkstatus/*: TON from the ENROLMENT only; "500"/"502" -> error.
#     Bites only in enrolment mode (agent tokens carry a benign 123 enrolment),
#     hence enrolment-502 -> backend 500 but agent-502 -> backend 200.
#   - 759/760/761: the stub maps RECOVERABLE_ERROR_* finals only for the
#     monthly-return poll, NOT for IR-CIS-VERIFY - the verify poll falls through
#     to the success response (stub gap). Kept here pinned to SUBMITTED so a
#     future stub extension flags itself by failing these rows.

ENROLMENT_SCENARIOS=(
  "123|EZ00100|happy path (success on poll)|202|ACCEPTED|SUBMITTED"
  "500|EZ00100|ChRIS 500 + taxpayer 500 on failure leg|500|FATAL_ERROR|-"
  "502|EZ00100|ChRIS 502 + govtalk 502 on failure leg|500|FATAL_ERROR|-"
  "503|EZ00100|ChRIS HTTP 503 on submit|200|FATAL_ERROR|-"
  "754|EZ00125|immediate FATAL_ERROR from ChRIS|200|FATAL_ERROR|-"
  "755|EZ00100|FATAL_ERROR on poll|202|ACCEPTED|FATAL_ERROR"
  "756|EZ00100|DEPARTMENTAL_ERROR on poll|202|ACCEPTED|DEPARTMENTAL_ERROR"
  "757|EZ00100|SUBMITTED_NO_RECEIPT on poll|202|ACCEPTED|SUBMITTED_NO_RECEIPT"
  "758|EZ00100|forever-pending ack (poll succeeds)|202|ACCEPTED|SUBMITTED"
  "759|EZ00100|recoverable 3000: stub gap for VERIFY|202|ACCEPTED|SUBMITTED"
  "760|EZ00100|recoverable 2005: stub gap for VERIFY|202|ACCEPTED|SUBMITTED"
  "761|EZ00100|recoverable 1000: stub gap for VERIFY|202|ACCEPTED|SUBMITTED"
)

AGENT_SCENARIOS=(
  "123|EZ00100|happy path (success on poll)|202|ACCEPTED|SUBMITTED"
  "500|EZ00100|ChRIS 500 + taxpayer 500 on failure leg|500|FATAL_ERROR|-"
  "502|EZ00100|ChRIS HTTP 502 on submit|200|FATAL_ERROR|-"
  "503|EZ00100|ChRIS HTTP 503 on submit|200|FATAL_ERROR|-"
  "754|EZ00125|immediate FATAL_ERROR from ChRIS|200|FATAL_ERROR|-"
  "755|EZ00100|FATAL_ERROR on poll|202|ACCEPTED|FATAL_ERROR"
  "756|EZ00100|DEPARTMENTAL_ERROR on poll|202|ACCEPTED|DEPARTMENTAL_ERROR"
  "757|EZ00100|SUBMITTED_NO_RECEIPT on poll|202|ACCEPTED|SUBMITTED_NO_RECEIPT"
  "758|EZ00100|forever-pending ack (poll succeeds)|202|ACCEPTED|SUBMITTED"
  "759|EZ00100|recoverable 3000: stub gap for VERIFY|202|ACCEPTED|SUBMITTED"
  "760|EZ00100|recoverable 2005: stub gap for VERIFY|202|ACCEPTED|SUBMITTED"
  "761|EZ00100|recoverable 1000: stub gap for VERIFY|202|ACCEPTED|SUBMITTED"
)

preflight
echo
echo "=== DTR-5655 verification e2e scenarios (backend $BACKEND_HOST -> stub $STUB_HOST) ==="

for mode in enrolment agent; do
  echo
  echo "--- $mode mode ---"
  if [[ "$mode" == "enrolment" ]]; then
    rows=("${ENROLMENT_SCENARIOS[@]}")
  else
    rows=("${AGENT_SCENARIOS[@]}")
  fi
  for row in "${rows[@]}"; do
    IFS='|' read -r ton tor label exp_http exp_status exp_poll <<<"$row"
    run_scenario "$mode" "$ton" "$tor" "$label" "$exp_http" "$exp_status" "$exp_poll"
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
