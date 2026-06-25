package com.codelry.util.capella;

import com.codelry.util.capella.exceptions.CapellaAPIError;
import com.couchbase.client.core.diagnostics.DiagnosticsResult;
import com.couchbase.client.core.env.*;
import com.couchbase.client.core.error.AmbiguousTimeoutException;
import com.couchbase.client.core.error.UnambiguousTimeoutException;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.diagnostics.PingOptions;
import com.couchbase.client.java.env.ClusterEnvironment;

import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Set;
import java.util.function.Consumer;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CapellaConnect {
  private static final Logger LOGGER = LogManager.getLogger(CapellaConnect.class);

  private static final Method CLUSTER_CONNECT_METHOD = retryMethod("clusterConnect", String.class, ClusterOptions.class);
  private static final Method BUCKET_WAIT_METHOD = retryMethod("bucketWait", Cluster.class, String.class);
  private static final Method CLUSTER_WAIT_METHOD = retryMethod("clusterWait", Cluster.class);

  private static Method retryMethod(String name, Class<?>... parameterTypes) {
    try {
      Method method = CapellaConnect.class.getMethod(name, parameterTypes);
      if (method.getAnnotation(Retryable.class) == null) {
        throw new IllegalStateException("Method " + name + " is not @Retryable");
      }
      return method;
    } catch (NoSuchMethodException e) {
      throw new ExceptionInInitializerError("Missing @Retryable method " + name + ": " + e);
    }
  }

  private static RuntimeException propagate(Exception e) {
    if (e instanceof RuntimeException re) {
      return re;
    }
    return new RuntimeException(e);
  }

  public static Cluster connect(CapellaCluster cluster) {
    String connectString = cluster.getConnectString();
    String username = cluster.getCredentials().getUsername();
    String password = cluster.getCredentials().getPassword();
    X509Certificate certificate = cluster.getCertificate().getCertificate();
    if (certificate == null) {
      try {
        certificate = cluster.getCertificate().getClusterCertificate();
        cluster.getCertificate().setCertificate(certificate);
      } catch (CapellaAPIError e) {
        throw new RuntimeException("Unable to fetch cluster certificate", e);
      }
    }
    final X509Certificate trustCertificate = certificate;
    PasswordAuthenticator authenticator = PasswordAuthenticator.create(username, password);
    Consumer<SecurityConfig.Builder> secConfiguration = securityConfig -> {
      try {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("capella-cluster", trustCertificate);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        securityConfig
            .enableTls(true)
            .trustManagerFactory(trustManagerFactory);
      } catch (Exception e) {
        throw new RuntimeException("Unable to configure cluster certificate trust", e);
      }
    };
    Consumer<IoConfig.Builder> ioConfiguration = ioConfig -> ioConfig
        .numKvConnections(4)
        .networkResolution(NetworkResolution.AUTO)
        .enableMutationTokens(false);
    Consumer<TimeoutConfig.Builder> timeOutConfiguration = timeoutConfig -> timeoutConfig
        .kvTimeout(Duration.ofSeconds(5))
        .connectTimeout(Duration.ofSeconds(15))
        .queryTimeout(Duration.ofSeconds(75));
    ClusterEnvironment environment = ClusterEnvironment
        .builder()
        .timeoutConfig(timeOutConfiguration)
        .ioConfig(ioConfiguration)
        .securityConfig(secConfiguration)
        .build();
    try {
      return clusterConnect(connectString, ClusterOptions.clusterOptions(authenticator).environment(environment));
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  @Retryable(allowableExceptions = {UnambiguousTimeoutException.class, AmbiguousTimeoutException.class})
  public static Cluster clusterConnect(String connectString, ClusterOptions options) {
    try {
      return RetryExecutor.execute(
          CLUSTER_CONNECT_METHOD,
          () -> {
            Cluster cluster = Cluster.connect(connectString, options);
            LOGGER.debug("Connected to {}", connectString);
            cluster.ping();
            DiagnosticsResult diagnostics = cluster.diagnostics();
            diagnostics.endpoints().forEach((serviceType, endpoints) -> {
              endpoints.forEach(ep -> LOGGER.debug("{} {} state={}", serviceType, ep.remote(), ep.state()));
            });
            return cluster;
          });
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  @Retryable(allowableExceptions = {UnambiguousTimeoutException.class, AmbiguousTimeoutException.class})
  public static void bucketWait(Cluster cluster, String bucketName) {
    try {
      RetryExecutor.executeVoid(
          BUCKET_WAIT_METHOD,
          () -> {
            cluster.ping();
            Bucket bucket = cluster.bucket(bucketName);
            bucket.waitUntilReady(Duration.ofSeconds(15));
          });
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  @Retryable(allowableExceptions = {UnambiguousTimeoutException.class, AmbiguousTimeoutException.class})
  public static void clusterWait(Cluster cluster) {
    try {
      RetryExecutor.executeVoid(
          CLUSTER_WAIT_METHOD,
          () -> {
            cluster.ping();
            cluster.waitUntilReady(Duration.ofSeconds(5));
          });
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  public static Collection collection(Cluster cluster, String bucketName) {
    return collection(cluster, bucketName, "_default", "_default");
  }

  public static Collection collection(Cluster cluster, String bucketName, String scopeName, String collectionName) {
    bucketWait(cluster, bucketName);
    return cluster.bucket(bucketName).scope(scopeName).collection(collectionName);
  }

  public static void disconnect(Cluster cluster) {
    ClusterEnvironment environment = cluster.environment();
    try {
      cluster.disconnect(Duration.ofSeconds(15));
    } catch (Exception e) {
      LOGGER.warn("Cluster disconnect did not complete cleanly");
    } finally {
      try {
        environment.shutdown(Duration.ofSeconds(15));
      } catch (Exception e) {
        LOGGER.warn("Environment shutdown did not complete cleanly");
      }
    }
  }
}
