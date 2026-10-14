package com.musicdata.charts.core;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.musicdata.charts.TrendingConfig;
import com.musicdata.charts.api.TrendingChart;
import com.musicdata.charts.api.TrendingEntry;
import com.musicdata.charts.db.ChartRepository;
import com.musicdata.charts.db.WindowCount;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the "trending now" chart for a country from the latest complete sliding window.
 *
 * <p>Every client in a country asks for the same chart, so results are cached for a few seconds
 * (Caffeine). At most one Cassandra read per country per TTL, whatever the request rate.
 */
public class TrendingService {

  private static final Comparator<WindowCount> BY_POPULARITY =
      Comparator.comparingLong(WindowCount::streams)
          .reversed()
          .thenComparing(Comparator.comparingLong(WindowCount::listeners).reversed())
          .thenComparing(WindowCount::trackId);

  private final ChartRepository repository;
  private final Clock clock;
  private final Duration window;
  private final Duration lag;
  private final int maxEntries;
  private final LoadingCache<String, TrendingChart> cache;

  public TrendingService(ChartRepository repository, Clock clock, TrendingConfig config) {
    this.repository = repository;
    this.clock = clock;
    this.window = Duration.ofMinutes(config.getWindowMinutes());
    this.lag = Duration.ofMinutes(config.getProcessingLagMinutes());
    this.maxEntries = config.getMaxEntries();
    this.cache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(config.getCacheTtlSeconds()))
            .maximumSize(1_000)
            .recordStats()
            .build(this::compute);
  }

  public TrendingChart trending(String country, int limit) {
    return cache.get(country).limit(limit);
  }

  TrendingChart compute(String country) {
    Instant start = latestWindowStart(clock.instant(), window, lag);
    List<WindowCount> rows = repository.trackWindowCounts(country, start);
    if (rows.isEmpty()) {
      // The streaming job may be one trigger behind; fall back one slide.
      start = start.minus(1, ChronoUnit.MINUTES);
      rows = repository.trackWindowCounts(country, start);
    }
    return new TrendingChart(
        country, start, start.plus(window), clock.instant(), rank(rows, maxEntries));
  }

  /** Windows slide every minute; the newest complete one ended {@code lag} ago. */
  static Instant latestWindowStart(Instant now, Duration window, Duration lag) {
    return now.truncatedTo(ChronoUnit.MINUTES).minus(window).minus(lag);
  }

  static List<TrendingEntry> rank(List<WindowCount> rows, int max) {
    List<WindowCount> sorted = new ArrayList<>(rows);
    sorted.sort(BY_POPULARITY);
    List<TrendingEntry> ranked = new ArrayList<>(Math.min(max, sorted.size()));
    for (int i = 0; i < sorted.size() && i < max; i++) {
      WindowCount c = sorted.get(i);
      ranked.add(new TrendingEntry(i + 1, c.trackId(), c.streams(), c.listeners()));
    }
    return List.copyOf(ranked);
  }
}
