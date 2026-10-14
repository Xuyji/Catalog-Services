-- Catalog metadata. One table per query; tracks_by_artist is a denormalized copy of tracks,
-- kept consistent by logged batches in CatalogService.upsertTrack.
CREATE KEYSPACE IF NOT EXISTS music_catalog
  WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1};

CREATE TABLE IF NOT EXISTS music_catalog.tracks (
  track_id      text PRIMARY KEY,
  isrc          text,
  title         text,
  artist_id     text,
  album_id      text,
  duration_ms   bigint,
  release_date  date,
  genres        set<text>,
  explicit      boolean
);

CREATE TABLE IF NOT EXISTS music_catalog.tracks_by_artist (
  artist_id     text,
  release_date  date,
  track_id      text,
  title         text,
  isrc          text,
  duration_ms   bigint,
  PRIMARY KEY ((artist_id), release_date, track_id)
) WITH CLUSTERING ORDER BY (release_date DESC, track_id ASC);

CREATE TABLE IF NOT EXISTS music_catalog.artists (
  artist_id  text PRIMARY KEY,
  name       text,
  country    text,
  genres     set<text>
);
