#!/bin/bash -x
set -x

echo "Deploying snapshot"
mvn deploy --settings travis/mvn-settings.xml -B -U -P sonatype-oss-release -DskipTests=true -Dmaven.test.skip=true -Dgpg.skip=true
