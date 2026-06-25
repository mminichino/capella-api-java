package com.codelry.util.capella;

import com.couchbase.client.core.util.ConnectionString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class CapellaConnectivity {
  private static final Logger LOGGER = LogManager.getLogger(CapellaConnectivity.class);

  private static final int MANAGER_PORT_TLS = 18091;
  private static final int MANAGER_PORT_PLAIN = 8091;
  private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(2);
  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
  private static final Duration DEFAULT_SRV_LOOKUP_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration DEFAULT_SRV_LOOKUP_POLL_INTERVAL = Duration.ofSeconds(2);

  private static final String SRV_SERVICE_PLAIN = "_couchbase._tcp.";
  private static final String SRV_SERVICE_TLS = "_couchbases._tcp.";

  private Duration srvLookupTimeout = DEFAULT_SRV_LOOKUP_TIMEOUT;
  private Duration srvLookupPollInterval = DEFAULT_SRV_LOOKUP_POLL_INTERVAL;

  public CapellaConnectivity() {}

  public CapellaConnectivity(Duration srvLookupTimeout, Duration srvLookupPollInterval) {
    this.srvLookupTimeout = srvLookupTimeout;
    this.srvLookupPollInterval = srvLookupPollInterval;
  }

  public CapellaConnectivity srvLookupRetry(Duration timeout, Duration pollInterval) {
    this.srvLookupTimeout = timeout;
    this.srvLookupPollInterval = pollInterval;
    return this;
  }

  public boolean checkConnectivity(String connectString, Duration timeout) {
    return checkConnectivity(connectString, true, timeout);
  }

  public boolean checkConnectivity(String connectString, boolean tls, Duration timeout) {
    return checkConnectivity(connectString, tls, timeout, DEFAULT_POLL_INTERVAL);
  }

  public boolean checkConnectivity(String connectString, boolean tls, Duration timeout, Duration pollInterval) {
    List<HostPort> targets = resolveTargets(connectString, tls);
    long deadline = System.nanoTime() + timeout.toNanos();

    while (System.nanoTime() < deadline) {
      for (HostPort target : targets) {
        if (canConnect(target.host(), target.port())) {
          LOGGER.debug("Connected to {}:{}", target.host(), target.port());
          return true;
        }
      }

      long remainingNanos = deadline - System.nanoTime();
      if (remainingNanos <= 0) {
        break;
      }
      long sleepMillis = Math.min(pollInterval.toMillis(), remainingNanos / 1_000_000);
      if (sleepMillis > 0) {
        try {
          Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return false;
        }
      }
    }

    LOGGER.debug("Connectivity check failed for {} within {}", connectString, timeout);
    return false;
  }

  List<HostPort> resolveTargets(String connectString, boolean tls) {
    ConnectionString cs = ConnectionString.create(connectString);
    int defaultPort = managerPort(tls);
    List<HostPort> targets = new ArrayList<>();

    for (ConnectionString.UnresolvedSocket host : cs.hosts()) {
      if (host.port() != 0) {
        targets.add(new HostPort(host.host(), host.port()));
        continue;
      }
      if (cs.isValidDnsSrv() && host.host().equals(cs.dnsSrvCandidate().orElse(null))) {
        targets.addAll(resolveSrvTargets(host.host(), tls, defaultPort));
      } else {
        targets.add(new HostPort(host.host(), defaultPort));
      }
    }
    return targets;
  }

  private List<HostPort> resolveSrvTargets(String hostname, boolean tls, int defaultPort) {
    long deadline = System.nanoTime() + srvLookupTimeout.toNanos();

    while (true) {
      try {
        List<String> hostnames = lookupSrvHostnames(hostname, tls);
        if (!hostnames.isEmpty()) {
          LOGGER.debug("SRV lookup succeeded for {}", hostname);
          return hostnames.stream().map(h -> new HostPort(h, defaultPort)).toList();
        }
        LOGGER.debug("SRV lookup returned no records for {}", hostname);
      } catch (NamingException e) {
        LOGGER.debug("SRV lookup failed for {}: {}", hostname, e.getMessage());
      }

      long remainingNanos = deadline - System.nanoTime();
      if (remainingNanos <= 0) {
        break;
      }
      long sleepMillis = Math.min(srvLookupPollInterval.toMillis(), remainingNanos / 1_000_000);
      if (sleepMillis > 0) {
        try {
          Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          LOGGER.debug("SRV lookup interrupted for {}, falling back to hostname", hostname);
          return List.of(new HostPort(hostname, defaultPort));
        }
      }
    }

    LOGGER.debug("SRV lookup timed out for {}, falling back to hostname", hostname);
    return List.of(new HostPort(hostname, defaultPort));
  }

  List<String> lookupSrvHostnames(String hostname, boolean tls) throws NamingException {
    String service = (tls ? SRV_SERVICE_TLS : SRV_SERVICE_PLAIN) + hostname;
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
    DirContext context = new InitialDirContext(env);
    Attributes attributes = context.getAttributes(service, new String[]{"SRV"});
    Attribute srv = attributes.get("SRV");
    if (srv == null) {
      return List.of();
    }

    List<String> targets = new ArrayList<>();
    for (int i = 0; i < srv.size(); i++) {
      String[] parts = srv.get(i).toString().split("\\s+");
      if (parts.length >= 4) {
        targets.add(parts[3].replaceAll("\\.$", ""));
      }
    }
    return targets;
  }

  private static int managerPort(boolean tls) {
    return tls ? MANAGER_PORT_TLS : MANAGER_PORT_PLAIN;
  }

  private static boolean canConnect(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), (int) DEFAULT_CONNECT_TIMEOUT.toMillis());
      return true;
    } catch (IOException e) {
      LOGGER.trace("Unable to connect to {}:{} - {}", host, port, e.getMessage());
      return false;
    }
  }

  record HostPort(String host, int port) {}
}
