cd /home/pn/Documents/code/forwardmeasure/forwardmeasure-platform
mvn -f reactor.xml -Pcontainer-image -Drat.skip=true -Dcontainer-image.push=true -DskipTests clean install
