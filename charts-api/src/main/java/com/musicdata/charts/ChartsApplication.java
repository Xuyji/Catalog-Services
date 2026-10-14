package com.musicdata.charts;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.musicdata.charts.core.TrendingService;
import com.musicdata.charts.db.CassandraChartRepository;
import com.musicdata.charts.health.CassandraHealthCheck;
import com.musicdata.charts.resources.ChartsResource;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import java.time.Clock;

/**
 * Charts API (Dropwizard): serves real-time trending charts, daily charts and per-track play-count
 * history from the Cassandra tables written by the streaming and batch pipelines.
 *
 * <pre>java -jar charts-api-1.0.0-all.jar server config.yml</pre>
 */
public class ChartsApplication extends Application<ChartsConfiguration> {

  public static void main(String[] args) throws Exception {
    new ChartsApplication().run(args);
  }

  @Override
  public String getName() {
    return "charts-api";
  }

  @Override
  public void initialize(Bootstrap<ChartsConfiguration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.getObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Override
  public void run(ChartsConfiguration config, Environment environment) {
    Clock clock = Clock.systemUTC();
    CqlSession session = config.getCassandra().build(environment.lifecycle());
    CassandraChartRepository repository = new CassandraChartRepository(session);
    TrendingService trending = new TrendingService(repository, clock, config.getTrending());

    environment.jersey().register(new ChartsResource(trending, repository, clock));
    environment.healthChecks().register("cassandra", new CassandraHealthCheck(session));
  }
}
