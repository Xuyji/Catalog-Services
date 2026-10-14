package com.musicdata.catalog.web;

import com.musicdata.catalog.model.Track;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record TrackRequest(
    /** ISO 3901 ISRC, e.g. USRC17607839. */
    @NotBlank @Pattern(regexp = "^[A-Z]{2}[A-Z0-9]{3}\\d{7}$") String isrc,
    @NotBlank @Size(max = 500) String title,
    @NotBlank String artistId,
    String albumId,
    @Positive @Max(7_200_000) long durationMs,
    @NotNull LocalDate releaseDate,
    @Size(max = 20) Set<@NotBlank String> genres,
    boolean explicit) {

  public Track toTrack(String trackId) {
    Set<String> normalizedGenres =
        genres == null
            ? Set.of()
            : genres.stream()
                .map(g -> g.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    return new Track(
        trackId,
        isrc,
        title.trim(),
        artistId,
        albumId,
        durationMs,
        releaseDate,
        normalizedGenres,
        explicit);
  }
}
