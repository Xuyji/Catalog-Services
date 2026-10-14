package com.musicdata.charts.db;

import com.musicdata.charts.api.DailyChartEntry;
import com.musicdata.charts.api.DailyTrackStat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Read access to the serving tables written by the music-streaming-pipeline jobs. */
public interface ChartRepository {

  List<WindowCount> trackWindowCounts(String country, Instant windowStart);

  List<DailyChartEntry> dailyTopTracks(String country, LocalDate date, int limit);

  /** Most recent first, back to and including {@code since}. */
  List<DailyTrackStat> dailyTrackStats(String trackId, LocalDate since);
}
