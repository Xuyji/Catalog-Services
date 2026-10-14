package com.musicdata.catalog.model;

import java.util.Set;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("artists")
public record Artist(
    @PrimaryKey("artist_id") String artistId, String name, String country, Set<String> genres) {}
