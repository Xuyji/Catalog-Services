package com.musicdata.charts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.musicdata.charts.api.DailyChartEntry;
import com.musicdata.charts.api.DailyTrackStat;
import com.musicdata.charts.api.TrendingChart;
import com.musicdata.charts.core.TrendingService;
import com.musicdata.charts.db.ChartRepository;
import com.musicdata.charts.db.WindowCount;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import com.musicdata.charts.resources.ChartsResource;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
class ChartsResourceTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-13T12:00:30Z"), ZoneOffset.UTC);
  // 12:00 truncated, minus 10-minute window, minus 1-minute lag.
  private static final Instant EXPECTED_WINDOW = Instant.parse("2026-10-13T11:49:00Z");

  static class FakeRepository implements ChartRepository {
    final AtomicInteger windowReads = new AtomicInteger();
    final List<Instant> requestedWindows = new ArrayList<>();
    LocalDate requestedSince;

    @Override
    public List<WindowCount> trackWindowCounts(String country, Instant windowStart) {
      windowReads.incrementAndGet();
      requestedWindows.add(windowStart);
      if (!windowStart.equals(EXPECTED_WINDOW)) {
        return List.of();
      }
      return List.of(
          new WindowCount("t3", 50, 40),
          new WindowCount("t1", 900, 700),
          new WindowCount("t2", 900, 800), // same streams as t1, more listeners -> ranks higher
          new WindowCount("t4", 10, 10));
    }

    @Override
    public List<DailyChartEntry> dailyTopTracks(String country, LocalDate date, int limit) {
      return List.of(new DailyChartEntry(1, "t9", "a1", 12345)).subList(0, Math.min(1, limit));
    }

    @Override
    public List<DailyTrackStat> dailyTrackStats(String trackId, LocalDate since) {
      requestedSince = since;
      return List.of(new DailyTrackStat(LocalDate.parse("2026-10-12"), 100, 80, 9_000_000, 0.2));
    }
  }

  private static final FakeRepository REPO = new FakeRepository();
  private static final TrendingService SERVICE = new TrendingService(REPO, CLOCK, new TrendingConfig());

  private static final ResourceExtension RESOURCES =
      ResourceExtension.builder().addResource(new ChartsResource(SERVICE, REPO, CLOCK)).build();

  @Test
  void trendingRanksByStreamsThenListenersAndAppliesLimit() {
    TrendingChart chart =
        RESOURCES.target("/v1/charts/trending/us").queryParam("limit", 3).request().get(TrendingChart.class);

    assertEquals("US", chart.country());
    assertEquals(EXPECTED_WINDOW, chart.windowStart());
    assertEquals(3, chart.entries().size());
    assertEquals("t2", chart.entries().get(0).trackId());
    assertEquals("t1", chart.entries().get(1).trackId());
    assertEquals(3, chart.entries().get(2).rank());
  }

  @Test
  void trendingIsServedFromCacheOnRepeatRequests() {
    int before = REPO.windowReads.get();
    for (int i = 0; i < 5; i++) {
      RESOURCES.target("/v1/charts/trending/GB").request().get(TrendingChart.class);
    }
    assertEquals(1, REPO.windowReads.get() - before);
  }

  @Test
  void invalidCountryIsRejected() {
    assertEquals(400, RESOURCES.target("/v1/charts/trending/USA").request().get().getStatus());
  }

  @Test
  void limitOutOfRangeIsRejected() {
    int status =
        RESOURCES.target("/v1/charts/trending/US").queryParam("limit", 5000).request().get().getStatus();
    assertEquals(400, status);
  }

  @Test
  void invalidDateIsRejected() {
    assertEquals(400, RESOURCES.target("/v1/charts/daily/US/yesterday").request().get().getStatus());
  }

  @Test
  void trackStatsLooksBackTheRequestedNumberOfDays() {
    int status =
        RESOURCES.target("/v1/tracks/t1/daily-stats").queryParam("days", 7).request().get().getStatus();
    assertEquals(200, status);
    assertEquals(LocalDate.parse("2026-10-07"), REPO.requestedSince);
  }
}
