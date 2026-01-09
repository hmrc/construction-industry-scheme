
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

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").