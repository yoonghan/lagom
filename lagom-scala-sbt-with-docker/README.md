# Description
This is a very simple Lagom program to test the deployment for lagom in Docker. The examples provided are either ConductR or Kubernetes but without pure Lagom on Docker.

## Steps
 1. Build docker via:
```
sbt clean docker:publishLocal
```
 2. Run docker images to locate for the built profile
```
docker images
```
 3. Run a docker with give port 9042
```
docker run -p9042:9042 cassandra
```
 4. Change the local pc ip of connection point in justice-league-impl/src/main/resources/application.conf
```
//Docker's Host IP, not docker's ip
 contact-points = ["192.168.1.244"]
```
 5. Execute docker with TAG given
```
docker run -p9000:9000 justice-league-impl:1.0-SNAPSHOT
```
 6. Execute in browser
```
http://localhost:9000/api/hero/GalGadot
```

## Issues encountered:
 *Problem*: Cluster Node [akka.tcp://application@172.17.0.4:2552] - No seed-nodes configured, manual cluster join required*
 *Solution*: Add seeds into application.conf. But because serverlocator runs in single cluster, I have added into
```
 lagom.defaults.cluster.join-self = on
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
