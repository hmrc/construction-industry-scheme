#!/usr/bin/env bash

sbt clean compile scalafmtCheckAll coverage test it/test coverageOff coverageReport
