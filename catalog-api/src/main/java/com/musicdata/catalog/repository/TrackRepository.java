package com.musicdata.catalog.repository;

import com.musicdata.catalog.model.Track;
import org.springframework.data.cassandra.repository.CassandraRepository;

public interface TrackRepository extends CassandraRepository<Track, String> {}
