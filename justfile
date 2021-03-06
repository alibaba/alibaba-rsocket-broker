# install rsocket-cli
setup_mac:
  brew tap AdoptOpenJDK/openjdk
  brew install adoptopenjdk-openjdk8
  brew install maven
  brew install yschimke/tap/rsocket-cli
  brew install docker-compose

# maven project 
build:
  mvn -DskipTests clean package

build-without-server:
  mvn -DskipTests -pl !alibaba-broker-server clean package

# install artifact into local repository
artifacts_install:
  mvn -pl .,alibaba-rsocket-service-common,alibaba-rsocket-core,alibaba-rsocket-spring-boot-starter,alibaba-broker-spring-boot-starter -DskipTests clean source:jar install

# build rsocket broker as Docker image
docker_build: artifacts_install
  mvn -pl alibaba-broker-server jib:dockerBuild

buildpack:
  mvn -DskipTests -pl alibaba-broker-server package spring-boot:build-image

# start rsocket broker
start_broker:
  java -jar alibaba-broker-server/target/alibaba-rsocket-broker.jar

debug_broker:
  java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar alibaba-broker-server/target/alibaba-rsocket-broker.jar

start_gossip_broker:
   java -jar -Drsocket.broker.topology=gossip -Drsocket.broker.seeds=192.168.11.11,192.168.11.12,192.168.11.13 alibaba-broker-server/target/alibaba-rsocket-broker.jar

# display outdated maven artifacts
updates:
   mvn compile versions:display-dependency-updates versions:display-plugin-updates > updates.txt

dependency_tree:
   mvn clean compile com.github.ferstl:depgraph-maven-plugin:3.3.0:aggregate -DgraphFormat=text -Dscope=compile -DshowVersions=true

staging:
   mvn -P release -DskipTests clean package deploy

deploy:
   mvn -DskipLocalStaging=true -P release -DskipTests clean package deploy

rsc-test:
   rsc --setupMetadata '{"ip":"127.0.0.1","name":"MockApp","sdk":"SpringBoot/2.3.7","device":"JavaApp"}' --setupMetadataMimeType "APP_INFO" tcp://localhost:9999 --request --route com.alibaba.user.UserService.findById -d '[1]' --debug

clean:
   mvn clean
   rm -rf alibaba-broker-server/node_modules
   rm -rf alibaba-broker-server/package*.json
   rm -rf alibaba-broker-server/webpack*.js