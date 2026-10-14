package com.musicdata.catalog.model;

import java.time.LocalDate;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * Denormalized copy of {@link Track} for "an artist's tracks, newest first":
 * {@code music_catalog.tracks_by_artist}. Kept in sync with {@code tracks} by logged batches.
 */
@Table("tracks_by_artist")
public record TrackByArtist(
    @PrimaryKeyColumn(name = "artist_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        String artistId,
    @PrimaryKeyColumn(
            name = "release_date",
            ordinal = 1,
            type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING)
        LocalDate releaseDate,
    @PrimaryKeyColumn(name = "track_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
        String trackId,
    String title,
    String isrc,
    @Column("duration_ms") long durationMs) {

  public static TrackByArtist of(Track t) {
    return new TrackByArtist(
        t.artistId(), t.releaseDate(), t.trackId(), t.title(), t.isrc(), t.durationMs());
  }
}
