# capella-api-java

Java wrapper for the [Couchbase Capella Management API](https://docs.couchbase.com/cloud/management-api-reference/index.html).
Create and manage organizations, projects, clusters, buckets, credentials, and allowed CIDRs, then connect with the
Couchbase Java SDK using the cluster certificate and database user created through the API.

- Java 17+
- Published to Maven Central as `com.codelry.util:capella-api`

## Install

Gradle:

```groovy
implementation 'com.codelry.util:capella-api:1.2.3'
```

Maven:

```xml
<dependency>
  <groupId>com.codelry.util</groupId>
  <artifactId>capella-api</artifactId>
  <version>1.2.3</version>
</dependency>
```

## Authentication

A Capella Management API v4 key is required. Create one in the Capella UI and keep the token somewhere the library can
read it. Project operations also need the Capella account email (or user ID) that owns the API key.

### Credentials file

For local use, store a profile under `~/.capella/credentials`:

```ini
[default]
api_host = cloudapi.cloud.couchbase.com
token_file = default-api-key-token.txt
organization = Field Engineering
project = demo
account_email = you@example.com

[junit]
api_host = cloudapi.cloud.couchbase.com
token_file = junit-api-key-token.txt
organization = Field Engineering
project = junit
account_email = you@example.com
```

Put the API key next to it, for example `~/.capella/default-api-key-token.txt`:

```
APIKeyToken: <token>
APIKeyId: <key-id>
```

If the token file is missing, the library will also look in `~/Downloads` and `~` and copy it into `~/.capella`.

Then initialize with a project name and optional profile (`default` if omitted):

```java
CouchbaseCapella capella = CouchbaseCapella.getInstance("demo", "default");
```

### Properties

For tests and automation, pass a `Properties` object. `capella.token` is required.

```java
Properties properties = new Properties();
properties.setProperty("capella.token", System.getenv("CAPELLA_TOKEN"));
properties.setProperty("capella.api.host", "cloudapi.cloud.couchbase.com");
properties.setProperty("capella.organization.name", "Field Engineering");
properties.setProperty("capella.project.name", "demo");
properties.setProperty("capella.database.name", "demo-cluster");
properties.setProperty("capella.user.email", "you@example.com");

CouchbaseCapella capella = CouchbaseCapella.getInstance(properties);
```

| Property | Purpose |
| --- | --- |
| `capella.token` | Management API v4 token (required for properties init) |
| `capella.api.host` | API host (default `cloudapi.cloud.couchbase.com`) |
| `capella.organization.name` / `capella.organization.id` | Organization to use (otherwise the first listed org) |
| `capella.project.name` / `capella.project.id` | Project to use (default name `default`; created if missing) |
| `capella.database.name` / `capella.database.id` | Existing cluster, or name used when creating one |
| `capella.user.email` / `capella.user.id` | Capella user used for project ownership |

## Typical workflow

Create a cluster, open network access, add a database user, create a bucket, then connect with the SDK:

```java
import com.codelry.util.capella.*;
import com.codelry.util.capella.logic.CloudType;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.bucket.BucketSettings;

import java.time.Duration;

CouchbaseCapella capella = CouchbaseCapella.getInstance(properties);
CapellaOrganization organization = CapellaOrganization.getInstance(capella);
CapellaProject project = organization.getDefaultProject();

CapellaCluster cluster = project.createCluster(
    "demo-cluster",
    new CapellaCluster.ClusterConfig().cloudType(CloudType.GCP));

cluster.getAllowedCIDR().createAllowedCIDR("0.0.0.0/0");
new CapellaConnectivity().checkConnectivity(cluster.getConnectString(), Duration.ofMinutes(2));

cluster.getCredentials().createCredential("developer", "#C0uchBas3", null);

CapellaBucket bucket = cluster.getBucket();
bucket.createBucket(BucketSettings.create("data").ramQuotaMB(128).numReplicas(1));

CapellaScope scope = CapellaScope.getInstance(bucket);
scope.createScope("group");

CapellaCollection collection = CapellaCollection.getInstance(scope);
collection.createCollection("table");

Cluster cbCluster = CapellaConnect.connect(cluster);
try {
  CapellaConnect.collection(cbCluster, "data", "group", "table")
      .upsert("test", JsonObject.create().put("id", "test"));
} finally {
  CapellaConnect.disconnect(cbCluster);
}
```

Create methods are idempotent: if the cluster, CIDR, credential, or bucket already exists, the existing resource is
reused. Cluster create waits until the cluster is healthy.

## Cluster configuration

`CapellaCluster.ClusterConfig` controls cloud, region, availability, and service groups. Defaults are AWS,
`us-east-2`, multi-zone, Developer Pro, and a 3-node data/query/index/search group (4 CPU, 16 GB RAM, 256 GB).

```java
import com.codelry.util.capella.logic.AvailabilityType;
import com.codelry.util.capella.logic.CloudType;
import com.codelry.util.capella.logic.SupportPlanType;

CapellaCluster cluster = project.createCluster(
    "demo-cluster",
    new CapellaCluster.ClusterConfig()
        .cloudType(CloudType.AWS)
        .cloudRegion("us-east-2")
        .availability(AvailabilityType.MULTI_ZONE)
        .supportPlan(SupportPlanType.DEVELOPER)
        .addServiceGroup(new CapellaCluster.ServiceGroupConfig()
            .cpu(4)
            .ram(16)
            .storage(256)
            .numOfNodes(3)
            .services(List.of("data", "query", "index", "search"))));
```

Single-node clusters (single zone, 100 GB) are available for development:

```java
project.createCluster("dev-cluster", new CapellaCluster.ClusterConfig().singleNode());
```

If you omit the cluster name, `capella.database.name` is used when set; otherwise a random name is generated.

```java
CapellaCluster cluster = project.createCluster(new CapellaCluster.ClusterConfig().cloudType(CloudType.AWS));
```

## Connect with the Couchbase SDK

`CapellaConnect` uses the cluster connection string, database credentials, and cluster CA certificate. Pass
`CapellaClusterConfig` to tune the SDK environment:

```java
CapellaClusterConfig config = new CapellaClusterConfig()
    .kvEndpoints(8)
    .kvTimeout(5)
    .connectTimeout(15)
    .queryTimeout(75)
    .maxHttpConnections(64)
    .enableMutationTokens(false)
    .build();

Cluster cbCluster = CapellaConnect.connect(cluster, config);
try {
  CapellaConnect.clusterWait(cbCluster);
  CapellaConnect.collection(cbCluster, "data")
      .upsert("doc-1", JsonObject.create().put("hello", "world"));
} finally {
  CapellaConnect.disconnect(cbCluster);
}
```

| Option | Default | Applied to |
| --- | --- | --- |
| `kvEndpoints` | `8` | KV connections per node |
| `kvTimeout` | `5` seconds | KV timeout |
| `connectTimeout` | `15` seconds | Connect timeout |
| `queryTimeout` | `75` seconds | Query timeout |
| `maxHttpConnections` | `64` | Max HTTP connections |
| `enableMutationTokens` | `false` | Mutation tokens |

`CapellaConnect.collection(...)` waits until the bucket is ready. Always call `CapellaConnect.disconnect` so the SDK
environment is shut down.

## Use an existing cluster

Attach a cluster by name (or by `capella.database.name` / `capella.database.id`) and supply database credentials
before connecting:

```java
CapellaCluster cluster = CapellaCluster.getInstance(project, "demo-cluster");
cluster.getCredentials().addCredentials("developer", "#C0uchBas3");

Cluster cbCluster = CapellaConnect.connect(cluster);
try {
  CapellaConnect.clusterWait(cbCluster);
} finally {
  CapellaConnect.disconnect(cbCluster);
}
```

## Database credentials

Passing `null` access grants `data_reader` and `data_writer` on all buckets. Scope access to a specific bucket:

```java
import com.codelry.util.capella.logic.DatabaseAccessEntry;
import com.codelry.util.capella.logic.DatabaseResourceBucketData;
import com.codelry.util.capella.logic.DatabaseResourceData;

cluster.getCredentials().createCredential(
    "app-user",
    "#C0uchBas3",
    List.of(new DatabaseAccessEntry(
        List.of("data_reader", "data_writer"),
        new DatabaseResourceData(List.of(new DatabaseResourceBucketData("data", null))))));
```

## App Services

Create an App Service on a cluster after it is healthy:

```java
import com.codelry.util.capella.logic.ComputeData;
import com.codelry.util.capella.logic.CreateAppServiceRequest;

cluster.getAppService().create(
    new CreateAppServiceRequest("sync", "App Services", 2, new ComputeData(2, 4), null));
```

## Cleanup

```java
bucket.delete();
cluster.delete();
```

Cluster delete waits until the cluster is destroyed.

## License

Apache-2.0
