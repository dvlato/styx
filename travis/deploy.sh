#!/usr/bin/env bash
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


set -e

echo "Deploying snapshot"
echo "Username=$SONATYPE_JIRA_USERNAME , password=$SONATYPE_JIRA_PASSWORD"
mvn deploy --settings travis/mvn-settings.xml -B -U -P sonatype-oss-release,linux -DskipTests=true  -Dmaven.test.skip=true -Dgpg.skip=true
