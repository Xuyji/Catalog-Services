package com.musicdata.catalog.service;

import com.musicdata.catalog.model.Artist;
import com.musicdata.catalog.model.Track;
import com.musicdata.catalog.model.TrackByArtist;
import com.musicdata.catalog.repository.ArtistRepository;
import com.musicdata.catalog.repository.TrackByArtistRepository;
import com.musicdata.catalog.repository.TrackRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

  private final TrackRepository tracks;
  private final ArtistRepository artists;
  private final TrackByArtistRepository tracksByArtist;
  private final CassandraOperations cassandra;

  public CatalogService(
      TrackRepository tracks,
      ArtistRepository artists,
      TrackByArtistRepository tracksByArtist,
      CassandraOperations cassandra) {
    this.tracks = tracks;
    this.artists = artists;
    this.tracksByArtist = tracksByArtist;
    this.cassandra = cassandra;
  }

  @Cacheable("tracks")
  public Optional<Track> getTrack(String trackId) {
    return tracks.findById(trackId);
  }

  @Cacheable("artists")
  public Optional<Artist> getArtist(String artistId) {
    return artists.findById(artistId);
  }

  public List<TrackByArtist> tracksByArtist(String artistId, int limit) {
    return tracksByArtist.findByArtistId(artistId, limit);
  }

  /**
   * Writes the track to both query tables in one <b>logged batch</b>, so the denormalized copies
   * never diverge. If the artist or release date changed, the old {@code tracks_by_artist} row
   * (which lives under a different primary key) is deleted in the same batch.
   */
  @CacheEvict(value = "tracks", key = "#track.trackId()")
  public Track upsertTrack(Track track) {
    CassandraBatchOperations batch = cassandra.batchOps();
    Optional<Track> existing = tracks.findById(track.trackId());
    if (existing.isPresent() && movedPartitionOrClustering(existing.get(), track)) {
      batch = batch.delete(TrackByArtist.of(existing.get()));
    }
    batch.insert(track).insert(TrackByArtist.of(track)).execute();
    return track;
  }

  static boolean movedPartitionOrClustering(Track before, Track after) {
    return !Objects.equals(before.artistId(), after.artistId())
        || !Objects.equals(before.releaseDate(), after.releaseDate());
  }
}
