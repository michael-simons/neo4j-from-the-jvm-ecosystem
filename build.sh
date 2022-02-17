#!/bin/bash

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
  "spring-boot25-with-sdn-ogm"
  "spring-data-imperative"
  "spring-data-imperative-module-path"
  "spring-data-imperative-native"
  "spring-data-reactive"
  "spring-plain-imperative"
  "spring-plain-reactive"
  "vertx-spring-data-reactive"
)
declare -t prefix=neo4j-from-the-jvm

for underTest in "${projects[@]}"; do
  printf "Building $underTest\n"
  
  if [[ $underTest = helidon*native ]]
  then
    underTest=${underTest%"-native"}
    (cd $underTest && mvn -DskipTests clean package && docker build -f Dockerfile.native --tag neo4j-from-the-jvm/$underTest-native:latest .)
      
  elif [[ $underTest = helidon* ]]
  then
    (cd $underTest && mvn -DskipTests clean package && docker build --tag neo4j-from-the-jvm/$underTest:latest .)

  elif [[ $underTest = micronaut*native ]]
  then
    underTest=${underTest%"-native"}
    (cd $underTest && ./mvnw -DskipTests clean package -Dpackaging=docker-native && docker image tag $underTest:latest neo4j-from-the-jvm/$underTest-native:latest)

  elif [[ $underTest = micronaut* ]]
  then
    (cd $underTest && ./mvnw -DskipTests clean package -Dpackaging=docker -Dimage=neo4j-from-the-jvm/$underTest:latest)

  elif [[ $underTest = quarkus-ogm-native ]]
  then
    underTest=${underTest%"-native"}
    (cd $underTest && ./mvnw -DskipTests clean package -Dquarkus.container-image.build=true -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-native-image:21.3-java17 -Dquarkus.container-image.group=neo4j-from-the-jvm -Dquarkus.container-image.tag=latest -Pnative -Dquarkus.native.container-build=true -Dquarkus.container-image.name=$underTest-native)

  elif [[ $underTest = quarkus*native ]]
  then
    underTest=${underTest%"-native"}
    (cd $underTest && ./mvnw -DskipTests clean package -Dquarkus.container-image.build=true -Dquarkus.container-image.group=neo4j-from-the-jvm -Dquarkus.container-image.tag=latest -Pnative -Dquarkus.native.container-build=true -Dquarkus.container-image.name=$underTest-native)

  elif [[ $underTest = quarkus* ]]
  then
    (cd $underTest && ./mvnw -DskipTests clean package -Dquarkus.container-image.build=true -Dquarkus.container-image.group=neo4j-from-the-jvm -Dquarkus.container-image.tag=latest)

  elif [[ $underTest = spring-data-imperative-module-path ]]
  then
    (cd $underTest && ./mvnw -DskipTests clean package && docker build --tag neo4j-from-the-jvm/$underTest:latest .)
    
  elif [[ $underTest = spring* ]]
  then
    (cd $underTest && ./mvnw -DskipTests clean spring-boot:build-image -Dspring-boot.build-image.imageName=$prefix/$underTest:latest)
    
  elif [[ $underTest = vertx-spring* ]]
  then
    (cd $underTest && ./mvnw -DskipTests clean spring-boot:build-image -Dspring-boot.build-image.imageName=$prefix/$underTest:latest)

  else
    echo "No match"
  fi
done
