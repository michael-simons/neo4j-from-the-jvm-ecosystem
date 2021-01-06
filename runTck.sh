#!/bin/bash

function finish {
  docker rm -f neo4j &>/dev/null
  docker network rm neo4j-tck &>/dev/null
}

trap finish EXIT

docker network create neo4j-tck &>/dev/null

docker run --name neo4j --publish=7687 -e 'NEO4J_AUTH=neo4j/secret' -d --network neo4j-tck neo4j:4.1 &>/dev/null
export NEO4J_BOLT=`docker inspect --format='{{(index (index .NetworkSettings.Ports "7687/tcp") 0).HostPort}}' neo4j` &>/dev/null
export NEO4J_IP=`docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' neo4j` &>/dev/null

(cd tck && ./mvnw -DskipTests clean test-compile package)
java -jar tck/target/tck-1.0.0-SNAPSHOT.jar --spring.neo4j.uri=bolt://localhost:$NEO4J_BOLT verifyConnection -t PT120S

declare -a projects=(
  "helidon-se-reactive"
  "helidon-se-reactive-native"
  "micronaut-imperative"
  "micronaut-reactive"
  "micronaut-reactive-native"
  "quarkus-imperative"
  "quarkus-imperative-native"
  "quarkus-ogm"
  "quarkus-ogm-native"
  "quarkus-reactive"
  "quarkus-reactive-native"
  "spring-boot23-with-sdn-ogm"
  "spring-boot24-with-sdn-ogm"
  "spring-data-imperative"
  "spring-data-imperative-native"
  "spring-data-reactive"
  "spring-plain-imperative"
  "spring-plain-reactive"
  "vertx-spring-data-reactive"
)
declare -t prefix=neo4j-from-the-jvm

for underTest in "${projects[@]}"; do
  {
    printf "Testing $underTest\n"

    if [[ $underTest = helidon* ]]
    then
      docker run --name underTest --publish=8080 -e "NEO4J_URI=bolt://$NEO4J_IP:7687" --network neo4j-tck -d $prefix/$underTest &>/dev/null

    elif [[ $underTest = micronaut* ]]
    then
      docker run --name underTest --publish=8080 -e 'NEO4J_URI=bolt://neo4j:7687' --network neo4j-tck -d $prefix/$underTest &>/dev/null

    elif [[ $underTest = quarkus* ]]
    then
      docker run --name underTest --publish=8080 -e 'QUARKUS_NEO4J_URI=bolt://neo4j:7687' --network neo4j-tck -d $prefix/$underTest &>/dev/null

    elif [[ $underTest = spring-boot23-* ]]
    then
      docker run --name underTest --publish=8080 -e 'ORG_NEO4J_DRIVER_URI=bolt://neo4j:7687' --network neo4j-tck -d $prefix/$underTest &>/dev/null

    elif [[ $underTest = spring* ]]
    then
      docker run --name underTest --publish=8080 -e 'SPRING_NEO4J_URI=bolt://neo4j:7687' --network neo4j-tck -d $prefix/$underTest &>/dev/null

    elif [[ $underTest = vertx-spring* ]]
    then
      docker run --name underTest --publish=8080 -e 'SPRING_NEO4J_URI=bolt://neo4j:7687' --network neo4j-tck -d $prefix/$underTest &>/dev/null

    else
      echo "No match"
    fi

    EXPOSED_PORT=`docker inspect --format='{{(index (index .NetworkSettings.Ports "8080/tcp") 0).HostPort}}' underTest`
    printf 'Waiting for container to start'
    until $(curl --output /dev/null --silent --head --fail-early http://localhost:$EXPOSED_PORT); do
      printf '.'
      sleep 0.1
    done
    printf '\n'
    (cd tck && SPRING_NEO4J_URI=bolt://localhost:$NEO4J_BOLT HOST_UNDER_TEST=localhost:$EXPOSED_PORT ./mvnw surefire:test)
    docker rm -f underTest &>/dev/null
  } || {
    docker rm -f underTest &>/dev/null
  }
done
