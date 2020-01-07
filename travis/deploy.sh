#!/bin/bash -x
set -x

echo "Deploying snapshot"
echo "Username=$SONATYPE_JIRA_USERNAME , password=$SONATYPE_JIRA_PASSWORD"
mvn clean deploy --settings travis/mvn-settings.xml -B -U -P sonatype-oss-release -DskipTests=true -Dmaven.test.skip=true -Dgpg.skip=true
