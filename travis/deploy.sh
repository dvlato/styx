#!/bin/bash -x
set -x

echo "Deploying snapshot"


if [ ! -n "$TRAVIS_TAG" ]; then
  echo "Deploying release"
  mvn deploy --settings travis/mvn-settings.xml -B -U -P sonatype-oss-release -DskipTests=true -Dmaven.test.skip=true -Dgpg.skip=false
else
  echo "Deploying snapshot"
  mvn deploy --settings travis/mvn-settings.xml -B -U -P sonatype-oss-release -DskipTests=true -Dmaven.test.skip=true -Dgpg.skip=true
fi
