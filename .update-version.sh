#!/bin/bash
version=${1}
sed -i "s/You are running WINGS version: .*/You are running WINGS version: ${version} /g" portal/src/main/webapp/html/home.html
cd core
mvn --settings pom.xml org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=${version}
exit 0