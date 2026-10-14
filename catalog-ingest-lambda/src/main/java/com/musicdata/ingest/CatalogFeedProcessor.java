package com.musicdata.ingest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Validates and normalizes a label's catalog delivery (JSON Lines, one track per line).
 *
 * <ul>
 *   <li>ISRCs are upper-cased with hyphens removed, then format-checked.
 *   <li>The track id is <b>derived from the ISRC</b>, so re-delivering a file is idempotent.
 *   <li>Duplicate ISRCs within a delivery: the last occurrence wins.
 *   <li>Invalid lines are kept with a reason so they can be quarantined, not silently dropped.
 * </ul>
 *
 * <p>Pure logic, no AWS calls: unit-tested without mocks.
 */
public final class CatalogFeedProcessor {

  private static final Pattern ISRC = Pattern.compile("^[A-Z]{2}[A-Z0-9]{3}\\d{7}$");
  private static final long MAX_DURATION_MS = 2 * 60 * 60 * 1000L;

  static final ObjectMapper MAPPER =
      new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

  public record NormalizedTrack(
      String trackId,
      String isrc,
      String title,
      String artistName,
      long durationMs,
      String releaseDate,
      List<String> genres,
      boolean explicit,
      String sourceFile) {}

  public record Rejected(int lineNumber, String reason, String rawLine) {}

  public record Result(List<NormalizedTrack> valid, List<Rejected> rejected) {}

  public Result process(Reader input, String sourceFile) {
    Map<String, NormalizedTrack> byIsrc = new LinkedHashMap<>();
    List<Rejected> rejected = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(input)) {
      String line;
      int lineNumber = 0;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.isBlank()) {
          continue;
        }
        try {
          NormalizedTrack track = normalize(MAPPER.readTree(line), sourceFile);
          byIsrc.remove(track.isrc()); // re-insert so "last wins" also keeps file order
          byIsrc.put(track.isrc(), track);
        } catch (InvalidRecordException e) {
          rejected.add(new Rejected(lineNumber, e.getMessage(), line));
        } catch (IOException e) {
          rejected.add(new Rejected(lineNumber, "malformed JSON", line));
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return new Result(List.copyOf(byIsrc.values()), List.copyOf(rejected));
  }

  static NormalizedTrack normalize(JsonNode node, String sourceFile) {
    String isrc = text(node, "isrc").replace("-", "").toUpperCase(Locale.ROOT);
    if (!ISRC.matcher(isrc).matches()) {
      throw new InvalidRecordException("invalid ISRC: " + isrc);
    }
    String title = text(node, "title").strip();
    if (title.isEmpty()) {
      throw new InvalidRecordException("missing title");
    }
    String artist = text(node, "artist_name").strip();
    if (artist.isEmpty()) {
      throw new InvalidRecordException("missing artist_name");
    }
    long duration = node.path("duration_ms").asLong(-1);
    if (duration <= 0 || duration > MAX_DURATION_MS) {
      throw new InvalidRecordException("duration_ms out of range: " + duration);
    }
    String releaseDate = text(node, "release_date");
    try {
      LocalDate.parse(releaseDate);
    } catch (DateTimeParseException e) {
      throw new InvalidRecordException("invalid release_date: " + releaseDate);
    }
    TreeSet<String> genres = new TreeSet<>();
    node.path("genres").forEach(g -> {
      String genre = g.asText().strip().toLowerCase(Locale.ROOT);
      if (!genre.isEmpty()) {
        genres.add(genre);
      }
    });
    return new NormalizedTrack(
        "t-" + isrc.toLowerCase(Locale.ROOT),
        isrc,
        title,
        artist,
        duration,
        releaseDate,
        List.copyOf(genres),
        node.path("explicit").asBoolean(false),
        sourceFile);
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? "" : value.asText();
  }

  static final class InvalidRecordException extends RuntimeException {
    InvalidRecordException(String message) {
      super(message);
    }
  }
}
