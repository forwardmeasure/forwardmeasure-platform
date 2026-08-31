#!/bin/bash

cd /home/pn/Documents/code/forwardmeasure/forwardmeasure-platform
set -eou pipefail

mvn -f reactor.xml -Pcontainer-image -Drat.skip=true -Dcontainer-image.push=true -Ddocker.nocache=true -DskipTests clean install

for i in $(docker images |grep forwardmeasure|awk '{ print $1 }')
do
	docker push $i
done
