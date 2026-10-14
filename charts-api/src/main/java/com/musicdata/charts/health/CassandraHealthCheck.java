package com.musicdata.charts.health;

import com.codahale.metrics.health.HealthCheck;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;

/** Exposed on the admin port at /healthcheck; drives the Kubernetes readiness probe. */
public class CassandraHealthCheck extends HealthCheck {

  private final CqlSession session;

  public CassandraHealthCheck(CqlSession session) {
    this.session = session;
  }

  @Override
  protected Result check() {
    Row row = session.execute("SELECT release_version FROM system.local").one();
    return row == null
        ? Result.unhealthy("no response from system.local")
        : Result.healthy("cassandra " + row.getString("release_version"));
  }
}
