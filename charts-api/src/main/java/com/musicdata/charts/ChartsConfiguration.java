package com.musicdata.charts;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class ChartsConfiguration extends Configuration {

  @Valid @NotNull private CassandraFactory cassandra = new CassandraFactory();

  @Valid @NotNull private TrendingConfig trending = new TrendingConfig();

  @JsonProperty
  public CassandraFactory getCassandra() {
    return cassandra;
  }

  @JsonProperty
  public void setCassandra(CassandraFactory cassandra) {
    this.cassandra = cassandra;
  }

  @JsonProperty
  public TrendingConfig getTrending() {
    return trending;
  }

  @JsonProperty
  public void setTrending(TrendingConfig trending) {
    this.trending = trending;
  }
}
