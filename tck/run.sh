#!/bin/bash

function finish {
  docker rm -f neo4j &>/dev/null
  docker network rm neo4j-tck &>/dev/null
}

trap finish EXIT

docker network create neo4j-tck &>/dev/null

docker run --name neo4j --publish=7687 -e 'NEO4J_AUTH=neo4j/secret' -d --network neo4j-tck neo4j:4.1 &>/dev/null
export NEO4J_BOLT=`docker inspect --format='{{(index (index .NetworkSettings.Ports "7687/tcp") 0).HostPort}}' neo4j` &>/dev/null

java src/main/java/org/neo4j/examples/jvm/tck/util/BoltHandshaker.java localhost $NEO4J_BOLT 120

# Declare a string array with type
declare -a projects=(
  "helidon-se-reactive"
  "micronaut-reactive"
  "quarkus-imperative"
  "quarkus-reactive"
  "spring-data-imperative"
  "spring-data-reactive"
  "spring-plain-imperative"
  "spring-plain-reactive"
)
declare -t prefix=neo4j-from-the-jvm

./mvnw -DskipTests clean test-compile

# Read the array values with space
for underTest in "${projects[@]}"; do
  {
    printf "Testing $underTest\n"

    if [[ $underTest = helidon* ]]
    then
      (cd ../$underTest && mvn -DskipTests clean package && docker build --tag neo4j-from-the-jvm/$underTest:latest .)
      docker run --name underTest --publish=8080 -e 'NEO4J_URI=bolt://neo4j:7687' --network neo4j-tck -d $prefix/$underTest &>/dev/null
      EXPOSED_PORT=`docker inspect --format='{{(index (index .NetworkSettings.Ports "8080/tcp") 0).HostPort}}' underTest`

    elif [[ $underTest = micronaut* ]]
    then
      (cd ../$underTest && ./mvnw -DskipTests clean package && docker build --tag neo4j-from-the-jvm/$underTest:latest .)
      docker run --name underTest --publish=8080 -e 'NEO4J_URI=bolt://neo4j:7687' --network neo4j-tck -d $prefix/$underTest &>/dev/null
      EXPOSED_PORT=`docker inspect --format='{{(index (index .NetworkSettings.Ports "8080/tcp") 0).HostPort}}' underTest`

    elif [[ $underTest = quarkus* ]]
    then
      (cd ../$underTest && ./mvnw -DskipTests clean package -Dquarkus.container-image.build=true -Dquarkus.container-image.group=neo4j-from-the-jvm -Dquarkus.container-image.tag=latest)
      docker run --name underTest --publish=8080 -e 'QUARKUS_NEO4J_URI=bolt://neo4j:7687' --network neo4j-tck -d $prefix/$underTest &>/dev/null
      EXPOSED_PORT=`docker inspect --format='{{(index (index .NetworkSettings.Ports "8080/tcp") 0).HostPort}}' underTest`

    elif [[ $underTest = spring* ]]
    then
      (cd ../$underTest && ./mvnw -DskipTests clean spring-boot:build-image -Dspring-boot.build-image.imageName=$prefix/$underTest:latest)
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
    SPRING_NEO4J_URI=bolt://localhost:$NEO4J_BOLT HOST_UNDER_TEST=localhost:$EXPOSED_PORT ./mvnw surefire:test
    docker rm -f underTest &>/dev/null
  } || {
    docker rm -f underTest &>/dev/null
  }
done