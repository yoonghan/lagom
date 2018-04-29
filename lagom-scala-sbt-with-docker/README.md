# Lagom on Docker Swarm

## Description
This is a very simple Lagom program to test the deployment for lagom in Docker Swarm.  The example below are divided into 2 projects.
* Justice-League - this project is a pure echo project, doing an echo of input to output
* Avengers - a more complex program that uses message broker to publish input to kafka and being retrieved later.

## Developer mode
1. Replace the tmp/application.conf file in both avengers-impl/src/main/resources and justice-league-impl/src/main/resources
2. Replacing application.conf are the most minimal calls and default all calls to build.sbt loaders.
3. Start the applcation with
```
sbt runAll
```
4. Within build.sbt there are these lines. Uncomment and change accordingly to point to external Kafka or Cassandra. Kafka or cassandra will not work once "false" line are uncommented.
```
//lagomKafkaEnabled in ThisBuild := false
//lagomKafkaAddress in ThisBuild := "localhost:9092"
//lagomCassandraEnabled in ThisBuild := false
//lagomUnmanagedServices in ThisBuild := Map("cas_native" -> "http://localhost:9042")
```


## Docker build steps
Presume that docker is installed on the machine.
 1. Build docker via, every different Operating system and processors, i.e. ARM, AMD, Intel needs a rebuild:
 ```
 sbt clean docker:publishLocal
 ```
 2. Run docker images to locate for the built profile
 ```
 docker images
 ```
 3. Run a docker cassandra with give port 9042
 ```
 docker run -p9042:9042 cassandra
 ```
 4. Run zookeeper with given port of 2181, below is default
 ```
 ./zkServer.sh start ../conf/zoo_sample.cfg
 ```
 5. Modify kafka's server.properties to make it server wide available
 ```
 listeners=PLAINTEXT://192.168.1.244:9092
 advertised.listeners=PLAINTEXT://192.168.1.244:9092
```
 6. Run kafka to zookeeper with given port 9092
 ```
 bin/kafka-topics.sh --list --zookeeper localhost:2181
 ```
 7. Change the local pc ip of connection point in justice-league-impl/src/main/resources/application.conf
```
//Cassandra's docker Host IP, not docker's ip
 contact-points = ["192.168.1.244"]
```
 8. Create a docker network
```
sudo docker network create --subnet=172.20.0.0/16 justicenetwork
```
 9. Execute docker with TAG given, for same server the -p9000:9000 needs to be removed. p2551 and host port need to change
```
sudo docker run -p9000:9000 \
-p2551:2551 \
--net justicenetwork --ip 172.20.0.2 \
-e BIND_IP=0.0.0.0 \
-e DB_HOST_IP=192.168.1.244 \
-e KAFKA_HOST_IP_AND_PORT=192.168.1.244:9092 \
-e CLUSTER_IP1=192.168.1.193 \
-e CLUSTER_IP2=192.168.1.38 -e CLUSTER_PORT2=2551 \
-e CLUSTER_IP3=192.168.1.4 -e CLUSTER_PORT3=2551 \
-e HOST_IP=192.168.1.193 -e HOST_PORT=2551 \
avengers-impl
```
 10. Execute in browser
```
http://localhost:9000/api/hero/GalGadot
```
 11. Stop docker
```
docker ps --all
docker rm <container-id>
```
 12. Create a docker compose which is a docker-compose.yml file
 ```
 #view docker-compose.yml
 version: '2'
 services:
  web:
    container_name: justice-league
    image: ${DB_HOST_IP}:5000/justice-league
    ports:
      - "9000:9000"
      - "2551:2551"
    environment:
      - BIND_IP=0.0.0.0
      - DB_HOST_IP=${DB_HOST_IP}
      - KAFKA_HOST_IP_AND_PORT=${KAFKA_HOST_IP_AND_PORT}
      - CLUSTER_IP1=${CLUSTER_HOST_IP1}
      - CLUSTER_IP2=${CLUSTER_HOST_IP2}
      - CLUSTER_IP3=${CLUSTER_HOST_IP3}
      - HOST_IP=${HOST_IP}
      - HOST_PORT=2551
    networks:
      justicenetwork:
        ipv4_address: 172.10.0.2
networks:
  justicenetwork:
    driver: bridge
    ipam:
      config:
        - subnet: 172.10.0.0/16
          gateway: 172.10.0.1
 ```
 13. Export DB_HOST_IP
 ```
  export DB_HOST_IP=192.168.1.244
  export KAFKA_HOST_IP_AND_PORT=192.168.1.244:9092
  export CLUSTER_HOST_IP1=192.168.1.38
  export CLUSTER_HOST_IP2=192.168.1.4
  export CLUSTER_HOST_IP3=192.168.1.193
  export HOST_IP=192.168.1.38
 ```
 14. Test with
```
 docker-compose up
```
 15. Check all ok, then Ctrl-C
```
 docker-compose down --volumes
 docker ps
 #remove all necessary
```
 16. Run docker's registery as a service
 ```
 docker service create --name registry --publish published=5000,target=5000 registry:2
 #Make sure the https/unsecure settings has been set for all hosts
 ```
 17. Publish lagom into registry
 ```
 docker tag justice-league-impl:1.0-SNAPSHOT 192.168.1.244:5000/justice-league
 docker push 192.168.1.244:5000/justice-league
 ```
 18. First version - to expose 1 web, but connected akka cluster
 ```
 version: '3'
 services:
   lagom-seed-1:
     image: ${DB_HOST_IP}:5000/justice-league
     ports:
       - "9000:9000"
     environment:
       - DB_HOST_IP=192.168.1.244
       - BIND_IP=0.0.0.0
       - CLUSTER_IP1=lagom-seed-1
       - CLUSTER_IP2=lagom-seed-2
       - CLUSTER_IP3=lagom-seed-3
       - HOST_IP=lagom-seed-1
       - HOST_PORT=2551
     networks:
       justicenetwork:
     deploy:
       replicas: 1
       resources:
         limits:
           cpus: "0.1"
           memory: 512M
       restart_policy:
         condition: on-failure
         delay: 20s
         max_attempts: 3
         window: 120s
   lagom-seed-2:
     image: ${DB_HOST_IP}:5000/justice-league
     environment:
       - DB_HOST_IP=192.168.1.244
       - BIND_IP=0.0.0.0
       - CLUSTER_IP1=lagom-seed-1
       - CLUSTER_IP2=lagom-seed-2
       - CLUSTER_IP3=lagom-seed-3
       - HOST_IP=lagom-seed-2
       - HOST_PORT=2552
     networks:
       justicenetwork:
     deploy:
       replicas: 1
       resources:
         limits:
           cpus: "0.1"
           memory: 512M
       restart_policy:
         condition: on-failure
         delay: 20s
         max_attempts: 3
         window: 120s
   lagom-seed-3:
     image: ${DB_HOST_IP}:5000/justice-league
     environment:
       - DB_HOST_IP=192.168.1.244
       - BIND_IP=0.0.0.0
       - CLUSTER_IP1=lagom-seed-1
       - CLUSTER_IP2=lagom-seed-2
       - CLUSTER_IP3=lagom-seed-3
       - HOST_IP=lagom-seed-3
       - HOST_PORT=2551
     networks:
       justicenetwork:
     deploy:
       replicas: 1
       resources:
         limits:
           cpus: "0.1"
           memory: 512M
       restart_policy:
         condition: on-failure
         delay: 20s
         max_attempts: 3
         window: 120s
 networks:
   justicenetwork:
 ```
 19. Start docker
  ```
  docker swarm init --advertise-addr 192.168.1.38
  # Run all the nodes with the provided command
  ```
 20. Suggest to see a visualizer, below codes only works for raspberry pi.
  ```
  docker service create \
  --name viz \
  --publish 8080:8080/tcp \
  --constraint node.role==manager \
  --mount type=bind,src=/var/run/docker.sock,dst=/var/run/docker.sock \
  alexellis2/visualizer-arm:latest
  ```
 21. Execute
 ```
 docker stack deploy -c docker-compose.yml dccomic
 # Check and see if replicas is 1.
 docker service ls
 # In the node where the service is running, do. The service can be check in visualizer
 docker container ls
 docker logs --follow <container id>
 ```

## Issues encountered:
 *Problem*: Cluster Node [akka.tcp://application@172.17.0.4:2552] - No seed-nodes configured, manual cluster join required*
 *Solution*: Add seeds into application.conf. But because serverlocator runs in single cluster, I have added into
```
 lagom.defaults.cluster.join-self = on

## Seeds should be open like this, there must be 2 or more seeds defined
#akka.cluster.seed-nodes = [
#  "akka.tcp://application@"${HOST_IP}":"${HOST_PORT},
#  "akka.tcp://application@"${CLUSTER_IP2}":"${CLUSTER_PORT2}
#  "akka.tcp://application@"${CLUSTER_IP3}":"${CLUSTER_PORT3}
#]
#akka {
#  remote {
#    netty.tcp {
#      hostname = ${HOST_IP}      # external (logical) hostname
#      port = ${HOST_PORT}                   # external (logical) port
#
#      bind-hostname = ${CLUSTER_IP} # internal (bind) hostname
#      bind-port = ${HOST_PORT}              # internal (bind) port
#    }
#}
#}
# Close this if seeds nodes are defined.
lagom.defaults.cluster.join-self = on
lagom.persistence.ask-timeout=30s


```

*Problem*: Ask timed out on [Actor[akka://application/system/sharding/JusticeLeagueEntity#-235261372]] after [5000 ms]. Sender[null] sent message of type "com.lightbend.lagom.scaladsl.persistence.CommandEnvelope".
*Solution*: Due to no JOINED seed, solution is as of above.

*Problem*: Failed to connect to Cassandra and initialize. It will be retried on demand. Caused by: No contact points for [cas_native]
*Solution*: As the build does not include a local cassandra, it needs to be referred externally. In doing so, we can let it connect via
```
cassandra.default {
  ## list the contact points  here
  contact-points = ["192.168.1.244"]
  ## override Lagom’s ServiceLocator-based ConfigSessionProvider
  session-provider = akka.persistence.cassandra.ConfigSessionProvider
}

cassandra-journal {
  contact-points = ${cassandra.default.contact-points}
  session-provider = ${cassandra.default.session-provider}
}

cassandra-snapshot-store {
  contact-points = ${cassandra.default.contact-points}
  session-provider = ${cassandra.default.session-provider}
}

lagom.persistence.read-side.cassandra {
  contact-points = ${cassandra.default.contact-points}
  session-provider = ${cassandra.default.session-provider}
}

```

*Problem*: Failed to connect to Cassandra and initialize. It will be retried on demand. Caused by: All host(s) tried for query failed (tried: /192.168.1.244:9042 (com.datastax.driver.core.exceptions.TransportException: [/192.168.1.244:9042] Cannot connect))
*Solution*: Exception locating cassandara, change the application.conf and recompile with the correct Host running docker ip.
```
cassandra.default {
 ## list the contact points  here, change to the host ip to connect to.
 contact-points = ["192.168.1.244"]
 ## override Lagom’s ServiceLocator-based ConfigSessionProvider
 session-provider = akka.persistence.cassandra.ConfigSessionProvider
}
```

*Problem*: Using seeds connection, the connection does not connect.
*Solution*: The seeds bind-host must be correctly set to the docker's internal ip. Do not use 127.0.0.1. To check the internal ip use:
```
docker inspect <container_id>
```
Else another solution is to create a docker network and assign static ip.
```
docker create network
```

*Problem*: Error after topic is created, Connection to node -1 could not be established. Broker may not be available.
*Solution*: Kafka not started, in MacOS, the /etc/hosts has to enable your hostname to point to 127.0.0.1

*Problem*: After message broker published a topic, an error appear after 60 minutes with "org.apache.kafka.common.errors.TimeoutException: Failed to update metadata after 60000 ms.". Of with subscription, the error appeared was "akka.actor.OneForOneStrategy [sourceThread=avengers-impl-application-akka.actor.default-dispatcher-3, akkaSource=akka://avengers-impl-application/system/sharding/kafkaProducer-Avengers/com.walcron.avengers.impl.AvengersTimelineEvent1/com.walcron.avengers.impl.AvengersTimelineEvent1/producer, sourceActorSystem=avengers-impl-application, akkaTimestamp=23:28:15.733UTC] - Failed to update metadata after 60000 ms."
*Solution*: Pointed to wrong kafka port, most probably it was pointing to Zookeeper and not Kafka. Change it, default port is 9092.
