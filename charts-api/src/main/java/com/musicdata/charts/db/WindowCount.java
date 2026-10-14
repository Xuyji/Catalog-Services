package com.musicdata.charts.db;

/** One row of {@code track_window_counts}: a track's stream count inside one sliding window. */
public record WindowCount(String trackId, long streams, long listeners) {}
