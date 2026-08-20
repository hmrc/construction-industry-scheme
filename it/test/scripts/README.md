# DTR-5655 end-to-end test scripts

> **Note:** the maintained scenario runner is now the Scala e2e suite —
> `sbt e2e/test`, see [`e2e/README.md`](../../../e2e/README.md). The scripts
> below remain available for ad-hoc manual calls (`bearer-token.sh`,
> `call-backend.sh`) and as a bash reference for the scenario matrix.

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

| Stub leg | Reads the TON from | Magic values (IR-CIS-VERIFY) |
|---|---|---|
| ChRIS submit/poll (XML) | the GovTalk XML envelope | 500–505 → that HTTP status; 779/EZ00125 → immediate fatal; 774–778 → terminal status on poll (775 fatal, 776 departmental 3001, 777 no-receipt, 778 forever pending) |
| `/cis-taxpayer` | the request body (resolved employer ref) | 500 → HTTP 500 |
| `/cis/govtalkstatus/*` | a `?scenario=` query param (not sent by the backend) | 500/502/404 |

The stub's verify success response contains one hardcoded subcontractor
(Noel Armstrong / DBB Construction / UTR 8786438047); the default request body
must describe the same subcontractor or the backend's
`VerificationResultMapper` fails the successful poll with HTTP 500.

Consequences encoded in the scenario tables of `run-e2e-scenarios.sh`:

- The verification flow derives the downstream employer ref from the **body**
  unconditionally (unlike monthly returns, which use `resolveEmployerRef`), so
  the `/cis-taxpayer` 500-magic only fires in agent mode: agent TON `500` →
  backend 500, enrolment TON `500` → backend 200 `FATAL_ERROR`.
- The govtalk failure leg is no longer reachable end-to-end (the backend never
  sends `?scenario=`), so no enrolment-502 special case remains.
- TON `780` (both modes) is the **F18 scenario 5** row: a 3000/`fatal`
  GovTalk error on the verify poll maps to `DEPARTMENTAL_ERROR` with
  `error.errorNumber=3000` (vs scenario 6 = 776 → 3001/`business`). Needs a
  stub that maps TON 780 to the `DEPARTMENTAL_ERROR_3000` final status backed
  by `submitCISVerifyMessage-departmentalError-3000-response.xml` (added to
  the stub 2026-07-13); against an older stub these rows fail with
  `poll status=SUBMITTED`.
- Scenario rows are labelled `F18 s<n>` after the tester's scenario numbers
  (s1 request>5xx, s2 request>GovTalk error, s3 poll>5xx, s4 poll>fatal,
  s5 poll>3000/fatal, s6 poll>3001/business, s7 poll>other error number —
  same mapper branch as s4, exercised by the 775 row via err 1001). **s3** has
  no TON: the suite polls a crafted URL
  (`…/poll/IR-CIS-VERIFY/2?final=SERVER_ERROR_500` — the stub only fires the
  5xx when the path count is ≥ 2) after a happy-path submit; the backend
  responds 200 `ACCEPTED` (keep-polling) with `govTalkErrorStatus=ServerError`.
- There are no recoverable (3000/2005/1000) entries in the verify statusMap at
  all — recoverable-on-poll is untestable for VERIFY until the stub adds them.
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
