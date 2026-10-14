package com.musicdata.charts.api;

import java.time.Instant;
import java.util.List;

public record TrendingChart(
    String country,
    Instant windowStart,
    Instant windowEnd,
    Instant generatedAt,
    List<TrendingEntry> entries) {

  /** Cached charts hold the max entries; each request takes its own prefix. */
  public TrendingChart limit(int limit) {
    if (entries.size() <= limit) {
      return this;
    }
    return new TrendingChart(
        country, windowStart, windowEnd, generatedAt, List.copyOf(entries.subList(0, limit)));
  }
}
