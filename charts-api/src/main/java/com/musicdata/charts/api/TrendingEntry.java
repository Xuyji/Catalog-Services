package com.musicdata.charts.api;

public record TrendingEntry(int rank, String trackId, long streams, long listeners) {}
