#!/bin/bash
set -e
version=${1}

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
	sed -i "s/You are running WINGS version: .*/You are running WINGS version: ${version} /g" portal/src/main/webapp/html/home.html
elif [[ "$OSTYPE" == "darwin"* ]]; then
        # Mac OSX
	if ! command -v gsed &> /dev/null
	then
	    echo "Please install gsed on macos"
	    exit 1
	fi
	gsed -i "s/You are running WINGS version: .*/You are running WINGS version: ${version} /g" portal/src/main/webapp/html/home.html
else
	echo "OS not supported"
        exit 1
fi
cd core
mvn --settings pom.xml org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=${version}
exit 0
