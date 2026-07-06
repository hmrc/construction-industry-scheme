# DTR-5655 end-to-end test scripts

## Problem statement

The DTR-5655 work adds verification routes to this backend that submit CIS
verification requests to ChRIS and poll for the outcome:

- `POST /cis/submissions/:submissionId/submit-verification-to-chris`
- `GET  /cis/submissions/verification/poll?pollUrl=...&submissionId=...`

For non-agent users (`isAgent=false`) the backend extracts `TaxOfficeNumber` /
`TaxOfficeReference` from the caller's `HMRC-CIS-ORG` enrolment and passes them
downstream into the ChRIS XML (the "enrolment pass-down"); for agents
(`isAgent=true`) they come from `clientTaxOfficeNumber` / `clientTaxOfficeRef`
in the request body. Unit and integration tests mock the downstreams, so we
also need a way to exercise the real chain locally:

```
bearer token (auth-login-api) -> backend auth -> enrolment/body -> ChRIS XML -> external stub
```

and to drive every error scenario the stub supports through that chain.

## Testing approach

The external stub (`../construction-industry-scheme-external-stub`, port 6997)
stands in for ChRIS, formp-proxy, rds-data-cache-proxy and email. It picks its
response based on "magic" TaxOfficeNumber values, so a single knob — the TON —
selects the scenario. These scripts place that TON either:

- **enrolment mode** (default): in the `HMRC-CIS-ORG` enrolment of a bearer
  token minted via auth-login-api (port 8585). This validates the DTR-5655
  enrolment pass-down, since the backend must carry the value from the
  enrolment into the ChRIS XML.
- **agent mode** (`-a`): in the request body (`clientTaxOfficeNumber`), with a
  benign `123` enrolment in the token.

For CISVERIFY the stub answers every non-5xx submit with an acknowledgement
(backend: `202 ACCEPTED`) whose poll URL encodes the terminal status
(`?final=FATAL_ERROR` etc.), so the suite chains submit → poll and asserts the
status of both legs.

One scenario TON exercises several stub legs with *different* dispatch rules:

| Stub leg | Reads the TON from | Magic values |
|---|---|---|
| ChRIS submit/poll (XML) | the GovTalk XML envelope | 500–505 → that HTTP status; 754/EZ00125 → immediate fatal; 755–758 → terminal status on poll |
| `/cis-taxpayer` | the request body (resolved employer ref) | 500 → HTTP 500 |
| `/cis/govtalkstatus/*` | the caller's **enrolment** | 500 → 500, 502 → 502 |

Consequences encoded in the scenario tables of `run-e2e-scenarios.sh`:

- TON `500` always ends as backend HTTP 500: the ChRIS failure handling itself
  calls `/cis-taxpayer`, which also fails.
- TON `502` differs by mode: enrolment mode → backend 500 (the govtalk leg
  fails too), agent mode → backend 200 with `status=FATAL_ERROR`.
- TON `759`/`760`/`761` (recoverable errors) are a **stub gap** for the VERIFY
  poll: only the CIS300MR monthly-return poll maps `RECOVERABLE_ERROR_*`
  responses, so the verify poll falls through to success. The suite pins these
  rows to `SUBMITTED` so they start failing (and get updated) if the stub gains
  support.
- A token with **no** `HMRC-CIS-ORG` enrolment and `isAgent=false` → backend
  500 (missing enrolment identifiers), covered as a negative test.

## Prerequisites

All running locally:

| Service | Port | How |
|---|---|---|
| this backend, **from the branch under test** | 6994 | `sbt run` (stop the sm2 copy first: `sm2 --stop CONSTRUCTION_INDUSTRY_SCHEME` — it serves the published release without the new routes) |
| construction-industry-scheme-external-stub | 6997 | `sbt run` in the parallel checkout |
| auth-login-api | 8585 | `sm2 --start AUTH_LOGIN_API` (or `CIS_ALL`) |
| auth | 8500 | sm2 |
| MongoDB | 27017 | — |

`jq` is required by the scenario suite (`brew install jq`).

## Script usage

### `bearer-token.sh [-n] [taxOfficeNumber] [taxOfficeReference]`

Mints a bearer token via auth-login-api and prints the `Authorization` header
value. Defaults: `123` / `EZ00100`. `-n` logs in **without** the `HMRC-CIS-ORG`
enrolment (negative testing).

```bash
./bearer-token.sh                 # token with 123/EZ00100 enrolment
./bearer-token.sh 502 EZ00100     # token whose enrolment triggers the 502 scenario
./bearer-token.sh -n              # token without the CIS enrolment
```

### `call-backend.sh [options]`

Single parameterized call against the backend (default
`http://localhost:6994`, override with `BACKEND_HOST`). Fetches a token
automatically. Without options it submits a valid verification request as a
non-agent and prints the HTTP status (stderr) and response body (stdout).

| Option | Meaning |
|---|---|
| `-t <num>` | TaxOfficeNumber driving the stub scenario (default `123`) |
| `-r <ref>` | TaxOfficeReference (default `EZ00100`) |
| `-a` | agent mode: `-t`/`-r` go into the body, token gets a benign enrolment |
| `-n` | token without the `HMRC-CIS-ORG` enrolment |
| `-s <id>` | submissionId (default `e2e-<epoch>`) |
| `-p <url>` | poll mode: `GET /cis/submissions/verification/poll` for the given pollUrl |
| `-e <path>` | explicit endpoint path (for other routes) |
| `-b <json>` | custom request body |
| `-X <verb>` | HTTP method override |
| `-o <file>` | write response body to file (default `/tmp/backend-response-body.txt`) |
| `-c` | print only the HTTP status code on stdout (body goes to the `-o` file) |

```bash
./call-backend.sh                          # happy path: 202 ACCEPTED + poll URL
./call-backend.sh -t 502                   # ChRIS 502 via the enrolment
./call-backend.sh -a -t 502                # same scenario via the agent/body path
./call-backend.sh -n                       # missing enrolment -> 500
# chain a poll using the responseEndPoint.url from a 202 response:
./call-backend.sh -s e2e-123 \
  -p 'http://localhost:6997/submission/ChRIS/poll/IR-CIS-VERIFY/0?final=FATAL_ERROR'
```

### `run-e2e-scenarios.sh`

Runs the whole matrix: preflights all services, executes every scenario in both
enrolment and agent mode (plus the no-enrolment negative test), chains
submit → poll where a poll URL is returned, and asserts the expected HTTP code
and `status` of each leg. Prints per-scenario PASS/FAIL and a summary; exits
non-zero on any failure.

```bash
./run-e2e-scenarios.sh
```

Expected outcomes per scenario live in `ENROLMENT_SCENARIOS` /
`AGENT_SCENARIOS` at the top of the script, with comments explaining the
mode-specific differences.
