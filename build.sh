mvn -Drat.skip=true -DskipTests -Pcontainer-image -Dcontainer-image.tag=1.0.0 -Dquarkus.container-image.push=true clean install
