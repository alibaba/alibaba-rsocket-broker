# maven package without test
build:
  mvn -DskipTests clean package

# build without server: rsocket server build is some slow
build-without-server:
  mvn -DskipTests -pl !alibaba-broker-server clean package

# install artifact into local repository
artifacts-install:
  mvn -pl .,alibaba-rsocket-service-common,alibaba-rsocket-core,alibaba-rsocket-spring-boot-starter,alibaba-broker-spring-boot-starter -DskipTests clean source:jar install

# install artifact into local repository
config-and-registry-install:
  mvn -pl alibaba-broker-config-client-spring-boot-starter,alibaba-broker-registry-client-spring-boot-starter -DskipTests clean source:jar install

# start rsocket broker
start-broker:
  java -jar alibaba-broker-server/target/alibaba-rsocket-broker.jar

debug-broker:
  java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar alibaba-broker-server/target/alibaba-rsocket-broker.jar

start-gossip-broker:
   java -jar -Drsocket.broker.topology=gossip -Drsocket.broker.seeds=192.168.11.11,192.168.11.12,192.168.11.13 alibaba-broker-server/target/alibaba-rsocket-broker.jar

# display outdated maven artifacts
updates:
   mvn compile versions:display-dependency-updates versions:display-plugin-updates > updates.txt

dependency-tree:
   mvn clean compile com.github.ferstl:depgraph-maven-plugin:3.3.0:aggregate -DgraphFormat=text -Dscope=compile -DshowVersions=true

dependencies:
   mvn compile dependency:tree > dependencies.txt

deploy-artifacts:
  mvn -P release -pl .,alibaba-rsocket-service-common,alibaba-rsocket-core,alibaba-rsocket-spring-boot-starter,alibaba-broker-spring-boot-starter -DskipTests clean package deploy

deploy-config-registry:
  mvn -P release -pl alibaba-broker-config-client-spring-boot-starter,alibaba-broker-registry-client-spring-boot-starter -DskipTests package deploy

deploy-http-gateway:
  mvn -P release -pl alibaba-broker-http-gateway -DskipTests package deploy

deploy-grpc-gateway:
  mvn -P release -pl alibaba-broker-grpc-gateway -DskipTests package deploy

deploy-broker-server:
  mvn -P release -pl alibaba-broker-server -DskipTests package deploy

buildpacks-build:
  mvn -pl alibaba-broker-server -DskipTests package spring-boot:build-image

# build rsocket broker as Docker image
jib-docker-build:
  mvn -pl alibaba-broker-server jib:dockerBuild

k8s-buildpacks:
  mvn -Pk8s -pl alibaba-broker-server -DskipTests package spring-boot:build-image

rsc-test:
   rsc --setupMetadata '{"ip":"127.0.0.1","name":"MockApp","sdk":"SpringBoot/2.4.5","device":"JavaApp"}' --setupMetadataMimeType "APP_INFO" tcp://localhost:9999 --request --route com.alibaba.user.UserService.findById -d '[1]' --debug

broker-test:
   rsc --setupMetadata '{"ip":"127.0.0.1","name":"MockApp","sdk":"SpringBoot/2.4.5","device":"rsc-cli"}' --setupMetadataMimeType "APP_INFO" tcp://localhost:9999 --request --route com.alibaba.rsocket.discovery.DiscoveryService.getInstances -d '["*"]' | jq

lint:
  mvn compile spotbugs:check
  mvn compile spotbugs:gui

clean:
   mvn clean
   rm -rf alibaba-broker-server/node_modules
   rm -rf alibaba-broker-server/package*.json
   rm -rf alibaba-broker-server/webpack*.js
   rm -rf alibaba-broker-server/vite*.ts
   rm -rf alibaba-broker-server/tsconfig.json
   rm -rf alibaba-broker-server/types.d.ts

# install rsocket-cli
setup-mac:
  brew tap AdoptOpenJDK/openjdk
  brew install adoptopenjdk-openjdk8
  brew install maven
  brew install yschimke/tap/rsocket-cli
  brew install docker-compose
