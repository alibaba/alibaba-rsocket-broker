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

# install artifact into local repository
artifacts_install: build
  mvn -pl .,alibaba-rsocket-core,alibaba-rsocket-spring-boot-starter,alibaba-broker-spring-boot-starter -DskipTests install

# build rsocket broker as Docker image
docker_build: artifacts_install
  mvn -pl alibaba-broker-server jib:dockerBuild

# start rsocket broker
start_broker:
  java -jar alibaba-broker-server/target/alibaba-broker-server.jar

clean:
   mvn clean
   rm -rf alibaba-broker-server/node_modules
   rm -rf alibaba-broker-server/package*.json
   rm -rf alibaba-broker-server/webpack*.js