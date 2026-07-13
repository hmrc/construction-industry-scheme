#!/bin/bash
set -euo pipefail

# Mints a local bearer token via auth-login-api (port 8585).
#
# Usage: bearer-token.sh [-n] [TAX_OFFICE_NUMBER] [TAX_OFFICE_REFERENCE]
#   -n   log in WITHOUT the HMRC-CIS-ORG enrolment (for negative testing of
#        the enrolment-driven flows)
#
# The TaxOfficeNumber placed in the enrolment is what drives the stub error
# scenarios for non-agent submissions (see run-e2e-scenarios.sh).

NO_ENROLMENT=0

while getopts "nh" opt; do
  case "$opt" in
    n) NO_ENROLMENT=1 ;;
    h)
      echo "Usage: $(basename "$0") [-n] [taxOfficeNumber] [taxOfficeReference]"
      exit 0
      ;;
    *) exit 1 ;;
  esac
done
shift $((OPTIND - 1))

TAX_OFFICE_NUMBER="${1:-123}"
TAX_OFFICE_REFERENCE="${2:-EZ00100}"
AUTH_URL="http://localhost:8585/government-gateway/session/login"

if [[ "$NO_ENROLMENT" -eq 1 ]]; then
  ENROLMENTS='[]'
else
  ENROLMENTS='[
      {
        "key": "HMRC-CIS-ORG",
        "identifiers": [
          { "key": "TaxOfficeNumber", "value": "'"$TAX_OFFICE_NUMBER"'" },
          { "key": "TaxOfficeReference", "value": "'"$TAX_OFFICE_REFERENCE"'" }
        ],
        "state": "Activated"
      }
    ]'
fi

response=$(curl -s -D - -o /dev/null -X POST "$AUTH_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "credId": "test-cred-id",
    "affinityGroup": "Organisation",
    "confidenceLevel": 200,
    "credentialStrength": "strong",
    "enrolments": '"$ENROLMENTS"'
  }')

token=$(echo "$response" | grep -i '^Authorization:' | sed 's/^[Aa]uthorization: *//' | tr -d '\r\n')

if [[ -z "$token" ]]; then
  echo "ERROR: Failed to obtain bearer token. Is auth-login-api running on port 8585?" >&2
  exit 1
fi

echo "$token"
