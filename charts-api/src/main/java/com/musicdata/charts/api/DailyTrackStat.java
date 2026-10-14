package com.musicdata.charts.api;

import java.time.LocalDate;

public record DailyTrackStat(
    LocalDate date, long streams, long uniqueListeners, long totalMsPlayed, double skipRate) {}
