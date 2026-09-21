construction-industry-scheme
============================
![](https://img.shields.io/github/v/release/hmrc/construction-industry-scheme)

Backend microservice for the Construction Industry Scheme (CIS), supporting contractors and their agents in managing and submitting monthly returns.

It is responsible for:

* Retrieving and managing monthly returns (standard, nil, and amended)
* Submitting returns to CHRIS (HMRC's backend filing system) via formp-proxy
* Managing subcontractor records and verification batches
* Supporting agent–client relationships and client list retrieval
* Caching session and journey data in MongoDB

## Running the service

Start the full service profile using Service Manager:

```bash
sm2 --start CIS_ALL
```

To run the service locally:

```bash
sbt run
```

The service runs on port **6994** by default.

### Upstream dependencies

| Service                   | Port |
|---------------------------|------|
| `auth`                    | 8500 |
| `internal-auth`           | 8470 |
| `formp-proxy`             | 6995 |
| `rds-datacache-proxy`     | 6992 |
| `chris`                   | 6997 |
| `client-exchange-proxy`   | 6997 |
| `email`                   | 8300 |

## Running the service against the real CIS filing database

To run without external stubs (pointing at the real `cis-filing-db`):

```bash
sm2 --start CIS_ALL_NO_DB_STUBS
```

Then start the service with the no-stubs configuration:

```bash
sbt -Dconfig.resource=application.no.stubs.conf run
```

### Registering the internal-auth token

When running with `application.no.stubs.conf` (or `application.conf`), the service authenticates with `formp-proxy` using an internal-auth token. The local internal-auth stub (port 8470) does not pre-register this token, so requests to `formp-proxy` will return `401 Invalid token` until you register it.

After internal-auth is running, execute the following once per internal-auth restart:

```bash
curl -s -X POST http://localhost:8470/test-only/token \
  -H "Content-Type: application/json" \
  -d '{"token":"6e8f4e4c-1f1e-4a1e-9f2a-3a5b7c9d0e1f","principal":"construction-industry-scheme","permissions":[{"resourceType":"formp-proxy","resourceLocation":"formp-proxy/cis","actions":["*"]}]}'
```

A successful response returns the token and an expiry date. The token persists in internal-auth's MongoDB until the service is restarted.

## Running the tests

To run all tests and generate a coverage report:

```bash
./run_all_tests.sh
```

Or run unit and integration tests individually with SBT:

```bash
sbt clean compile test it/test
```

## License

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
