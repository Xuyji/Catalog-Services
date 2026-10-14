package com.musicdata.charts.db;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.musicdata.charts.api.DailyChartEntry;
import com.musicdata.charts.api.DailyTrackStat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Every query is a single-partition read on a prepared statement: no scatter-gather across nodes,
 * and the driver's token-aware routing sends it straight to a replica.
 */
public class CassandraChartRepository implements ChartRepository {

  private final CqlSession session;
  private final PreparedStatement windowCounts;
  private final PreparedStatement dailyTop;
  private final PreparedStatement trackStats;

  public CassandraChartRepository(CqlSession session) {
    this.session = session;
    this.windowCounts =
        session.prepare(
            "SELECT track_id, streams, listeners FROM track_window_counts "
                + "WHERE country = ? AND window_start = ?");
    this.dailyTop =
        session.prepare(
            "SELECT rank, track_id, artist_id, streams FROM daily_top_tracks "
                + "WHERE country = ? AND event_date = ? LIMIT ?");
    this.trackStats =
        session.prepare(
            "SELECT event_date, streams, unique_listeners, total_ms_played, skip_rate "
                + "FROM daily_track_stats WHERE track_id = ? AND event_date >= ?");
  }

  @Override
  public List<WindowCount> trackWindowCounts(String country, Instant windowStart) {
    List<WindowCount> out = new ArrayList<>();
    for (Row row : session.execute(windowCounts.bind(country, windowStart))) {
      out.add(
          new WindowCount(
              row.getString("track_id"), row.getLong("streams"), row.getLong("listeners")));
    }
    return out;
  }

  @Override
  public List<DailyChartEntry> dailyTopTracks(String country, LocalDate date, int limit) {
    List<DailyChartEntry> out = new ArrayList<>();
    for (Row row : session.execute(dailyTop.bind(country, date, limit))) {
      out.add(
          new DailyChartEntry(
              row.getInt("rank"),
              row.getString("track_id"),
              row.getString("artist_id"),
              row.getLong("streams")));
    }
    return out;
  }

  @Override
  public List<DailyTrackStat> dailyTrackStats(String trackId, LocalDate since) {
    List<DailyTrackStat> out = new ArrayList<>();
    for (Row row : session.execute(trackStats.bind(trackId, since))) {
      out.add(
          new DailyTrackStat(
              row.getLocalDate("event_date"),
              row.getLong("streams"),
              row.getLong("unique_listeners"),
              row.getLong("total_ms_played"),
              row.getDouble("skip_rate")));
    }
    return out;
  }
}
