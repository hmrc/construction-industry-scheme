# E2E test suite (`sbt e2e/test`)

This end-to-end test suite drives the full verification chain
against **locally running services**:

```
bearer token (auth-login-api) -> backend auth -> enrolment/body -> ChRIS XML -> external stub
```

Running every stub error scenario in enrolment mode (isAgent=false, scenario
TaxOfficeNumber in the `HMRC-CIS-ORG` enrolment — the DTR-5655 enrolment
pass-down) and agent mode (isAgent=true, TON in the request body), plus a
no-enrolment negative test. See `it/test/scripts/README.md` for the full
problem statement, the stub's magic-TON contract, and the manual curl-style
helper scripts (`bearer-token.sh`, `call-backend.sh`), which remain available
for ad-hoc poking. This Scala suite is the maintained scenario runner.

The suite is **opt-in**: the `e2e` sbt project is not aggregated by the root
project, so `sbt test`, `sbt it/test` and `./run_all_tests.sh` (CI) never run
it. It only runs when invoked explicitly.

## Prerequisites

All running locally (the suite does not start anything):

| Service | Port | How |
|---|---|---|
| this backend, **from the branch under test** | 6994 | `sbt run` (stop the sm2 copy first: `sm2 --stop CONSTRUCTION_INDUSTRY_SCHEME` — it serves the published release without the new routes) |
| construction-industry-scheme-external-stub | 6997 | `sbt run` in the parallel checkout |
| auth-login-api | 8585 | `sm2 --start AUTH_LOGIN_API` (or `CIS_ALL`) |
| auth | 8500 | sm2 |
| MongoDB | 27017 | — |

No `jq`/`curl`/bash needed — the suite uses the JDK HTTP client, so it runs the
same on macOS, Linux and WSL/Windows.

## Running

```bash
sbt e2e/test                                  # whole matrix (23 tests)
sbt "e2e/testOnly *EnrolmentModeE2eSpec"      # one mode only
```

If any required service is down, every test is **canceled** (not failed) with a
message listing what is unreachable and how to start it; the sbt task still
succeeds. Set `E2E_STRICT=true` to fail instead.

### Configuration (env vars, same names/defaults as the bash scripts)

| Variable | Default |
|---|---|
| `BACKEND_HOST` | `http://localhost:6994` |
| `STUB_HOST` | `http://localhost:6997` |
| `AUTH_HOST` | `http://localhost:8500` |
| `AUTH_LOGIN_API_HOST` | `http://localhost:8585` |
| `E2E_STRICT` | `false` |
