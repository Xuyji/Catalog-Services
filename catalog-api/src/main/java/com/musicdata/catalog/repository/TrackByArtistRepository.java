package com.musicdata.catalog.repository;

import com.musicdata.catalog.model.TrackByArtist;
import java.util.List;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

public interface TrackByArtistRepository extends CassandraRepository<TrackByArtist, String> {

  /** Single-partition read, already sorted newest-first by the clustering order. */
  @Query("SELECT * FROM tracks_by_artist WHERE artist_id = ?0 LIMIT ?1")
  List<TrackByArtist> findByArtistId(String artistId, int limit);
}
