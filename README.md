
# construction-industry-scheme

This is the new construction-industry-scheme repository

## Running the service

Service Manager: `sm2 --start CIS_ALL`

To run all tests and coverage: `./run_all_tests.sh`

To start the server locally: `sbt run`

## Running the service against the cis-filing-db and without the contruction-industry-scheme-external-stubs

Service Manager: `sm2 --start CIS_ALL_NO_DB_STUBS`

To run all tests and coverage: `./run_all_tests.sh`

To start the server locally: `sbt -Dconfig.resource=application.no.stubs.conf run`
This forces the service to use the application.no.stubs.conf file which points to the real cis-filing-db service.

### Registering the internal-auth token

When running locally with `application.no.stubs.conf` (or `application.conf`), the service authenticates with `formp-proxy` using an internal-auth token. The local internal-auth stub (port 8470) does not have this token pre-registered, so requests to `formp-proxy` will return 401 "Invalid token" until you register it.

After internal-auth is running, execute the following once per internal-auth restart:

```bash
curl -s -X POST http://localhost:8470/test-only/token \
  -H "Content-Type: application/json" \
  -d '{"token":"6e8f4e4c-1f1e-4a1e-9f2a-3a5b7c9d0e1f","principal":"construction-industry-scheme","permissions":[{"resourceType":"formp-proxy","resourceLocation":"formp-proxy/cis","actions":["*"]}]}'
```

A successful response returns the token and an expiry date. The token persists in internal-auth's MongoDB until the service is restarted.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").