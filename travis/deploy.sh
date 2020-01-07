#!/bin/bash -x
set -x

echo "Deploying snapshot"
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
mvn deploy --settings travis/mvn-settings.xml -B -U -P sonatype-oss-release,docker -DskipTests=true -Dmaven.test.skip=true -Dgpg.skip=false
docker push dlatorre/styx
