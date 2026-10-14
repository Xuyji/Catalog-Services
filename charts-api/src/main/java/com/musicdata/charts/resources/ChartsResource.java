package com.musicdata.charts.resources;

import com.codahale.metrics.annotation.Timed;
import com.musicdata.charts.api.DailyChartEntry;
import com.musicdata.charts.api.DailyTrackStat;
import com.musicdata.charts.api.TrendingChart;
import com.musicdata.charts.core.TrendingService;
import com.musicdata.charts.db.ChartRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
public class ChartsResource {

  private static final Pattern COUNTRY = Pattern.compile("^[A-Za-z]{2}$");
  private static final Pattern TRACK_ID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

  private final TrendingService trending;
  private final ChartRepository repository;
  private final Clock clock;

  public ChartsResource(TrendingService trending, ChartRepository repository, Clock clock) {
    this.trending = trending;
    this.repository = repository;
    this.clock = clock;
  }

  @GET
  @Path("/charts/trending/{country}")
  @Timed
  public TrendingChart trending(
      @PathParam("country") String country,
      @QueryParam("limit") @DefaultValue("50") @Min(1) @Max(200) int limit) {
    return trending.trending(country(country), limit);
  }

  @GET
  @Path("/charts/daily/{country}/{date}")
  @Timed
  public List<DailyChartEntry> daily(
      @PathParam("country") String country,
      @PathParam("date") String date,
      @QueryParam("limit") @DefaultValue("100") @Min(1) @Max(200) int limit) {
    return repository.dailyTopTracks(country(country), date(date), limit);
  }

  @GET
  @Path("/tracks/{trackId}/daily-stats")
  @Timed
  public List<DailyTrackStat> trackStats(
      @PathParam("trackId") String trackId,
      @QueryParam("days") @DefaultValue("30") @Min(1) @Max(365) int days) {
    if (!TRACK_ID.matcher(trackId).matches()) {
      throw new BadRequestException("invalid track id");
    }
    LocalDate since = LocalDate.now(clock.withZone(ZoneOffset.UTC)).minusDays(days - 1L);
    return repository.dailyTrackStats(trackId, since);
  }

  private static String country(String raw) {
    if (!COUNTRY.matcher(raw).matches()) {
      throw new BadRequestException("country must be an ISO 3166-1 alpha-2 code");
    }
    return raw.toUpperCase(Locale.ROOT);
  }

  private static LocalDate date(String raw) {
    try {
      return LocalDate.parse(raw);
    } catch (DateTimeParseException e) {
      throw new BadRequestException("date must be YYYY-MM-DD");
    }
  }
}
