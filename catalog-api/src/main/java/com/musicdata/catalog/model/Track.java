package com.musicdata.catalog.model;

import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/** Lookup by track id: {@code music_catalog.tracks}. */
@Table("tracks")
public record Track(
    @PrimaryKey("track_id") String trackId,
    String isrc,
    String title,
    @Column("artist_id") String artistId,
    @Column("album_id") String albumId,
    @Column("duration_ms") long durationMs,
    @Column("release_date") LocalDate releaseDate,
    Set<String> genres,
    boolean explicit) {}
