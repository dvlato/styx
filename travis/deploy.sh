#!/bin/bash
#
# Copyright (C) 2013-2020 Expedia Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

echo "Deploying snapshot"
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
mvn deploy --settings travis/mvn-settings.xml -B -U -P sonatype-oss-release,linux -DskipTests=true -Dmaven.test.skip=true -Dgpg.skip=true  -Dstyxcore.docker.image=dlatorre/styx
mvn install -f distribution/pom.xml -B -U -P docker -Dstyxcore.docker.image=dlatorre/styx
docker push dlatorre/styx