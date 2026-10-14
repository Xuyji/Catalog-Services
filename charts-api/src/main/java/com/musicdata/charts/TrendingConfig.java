package com.musicdata.charts;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class TrendingConfig {

  @Min(1) private int windowMinutes = 10;
  @Min(0) private int processingLagMinutes = 1;
  @Min(1) @Max(300) private int cacheTtlSeconds = 15;
  @Min(1) @Max(1000) private int maxEntries = 200;

  @JsonProperty
  public int getWindowMinutes() {
    return windowMinutes;
  }

  @JsonProperty
  public void setWindowMinutes(int windowMinutes) {
    this.windowMinutes = windowMinutes;
  }

  @JsonProperty
  public int getProcessingLagMinutes() {
    return processingLagMinutes;
  }

  @JsonProperty
  public void setProcessingLagMinutes(int processingLagMinutes) {
    this.processingLagMinutes = processingLagMinutes;
  }

  @JsonProperty
  public int getCacheTtlSeconds() {
    return cacheTtlSeconds;
  }

  @JsonProperty
  public void setCacheTtlSeconds(int cacheTtlSeconds) {
    this.cacheTtlSeconds = cacheTtlSeconds;
  }

  @JsonProperty
  public int getMaxEntries() {
    return maxEntries;
  }

  @JsonProperty
  public void setMaxEntries(int maxEntries) {
    this.maxEntries = maxEntries;
  }
}
