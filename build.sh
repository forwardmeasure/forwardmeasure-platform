#!/bin/bash

cd /home/pn/Documents/code/forwardmeasure/forwardmeasure-platform
mvn -f reactor.xml spotless:apply

set -eou pipefail

# Optional first arg: a -pl module path (relative to this reactor.xml, e.g.
# "../forwardmeasure-openworkflow/openworkflow-deployments/studio/quarkus")
# to scope the build to just that module and what it actually needs, via
# -am, instead of the full multi-repo reactor. No arg = unchanged full
# build, exactly as before.


if [ -n "${1:-}" ]; then
	mvn -f reactor.xml -pl "$1" -am -Pcontainer-image -Drat.skip=true -Dcontainer-image.push=true -Ddocker.nocache=true -DskipTests clean install
else
	mvn -f reactor.xml -Pcontainer-image -Drat.skip=true -Dcontainer-image.push=true -Ddocker.nocache=true -DskipTests clean install
fi

for i in $(docker images |grep forwardmeasure|awk '{ print $1 }')
do
	docker push $i
done
