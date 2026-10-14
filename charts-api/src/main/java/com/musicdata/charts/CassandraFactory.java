package com.musicdata.charts;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.net.InetSocketAddress;
import java.time.Duration;

/** Builds a {@link CqlSession} whose lifecycle is managed by Dropwizard (closed on shutdown). */
public class CassandraFactory {

  /** Comma-separated host:port list. */
  @NotBlank private String contactPoints = "localhost:9042";
  @NotBlank private String localDatacenter = "datacenter1";
  @NotBlank private String keyspace = "music_analytics";
  @Min(10) private int requestTimeoutMs = 500;

  public CqlSession build(LifecycleEnvironment lifecycle) {
    DriverConfigLoader driverConfig =
        DriverConfigLoader.programmaticBuilder()
            .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofMillis(requestTimeoutMs))
            // Reads stay inside the local DC; quorum there tolerates one replica being down.
            .withString(DefaultDriverOption.REQUEST_CONSISTENCY, "LOCAL_QUORUM")
            .withBoolean(DefaultDriverOption.REQUEST_DEFAULT_IDEMPOTENCE, true)
            .withString(
                DefaultDriverOption.SPECULATIVE_EXECUTION_POLICY_CLASS,
                "ConstantSpeculativeExecutionPolicy")
            .withInt(DefaultDriverOption.SPECULATIVE_EXECUTION_MAX, 2)
            .withDuration(DefaultDriverOption.SPECULATIVE_EXECUTION_DELAY, Duration.ofMillis(50))
            .build();

    var builder =
        CqlSession.builder()
            .withConfigLoader(driverConfig)
            .withLocalDatacenter(localDatacenter)
            .withKeyspace(keyspace);
    for (String hostPort : contactPoints.split(",")) {
      String[] parts = hostPort.trim().split(":");
      int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9042;
      builder.addContactPoint(new InetSocketAddress(parts[0], port));
    }
    CqlSession session = builder.build();
    lifecycle.manage(
        new Managed() {
          @Override
          public void stop() {
            session.close();
          }
        });
    return session;
  }

  @JsonProperty
  public String getContactPoints() {
    return contactPoints;
  }

  @JsonProperty
  public void setContactPoints(String contactPoints) {
    this.contactPoints = contactPoints;
  }

  @JsonProperty
  public String getLocalDatacenter() {
    return localDatacenter;
  }

  @JsonProperty
  public void setLocalDatacenter(String localDatacenter) {
    this.localDatacenter = localDatacenter;
  }

  @JsonProperty
  public String getKeyspace() {
    return keyspace;
  }

  @JsonProperty
  public void setKeyspace(String keyspace) {
    this.keyspace = keyspace;
  }

  @JsonProperty
  public int getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  @JsonProperty
  public void setRequestTimeoutMs(int requestTimeoutMs) {
    this.requestTimeoutMs = requestTimeoutMs;
  }
}
