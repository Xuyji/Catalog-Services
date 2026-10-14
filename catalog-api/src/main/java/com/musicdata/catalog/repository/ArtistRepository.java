package com.musicdata.catalog.repository;

import com.musicdata.catalog.model.Artist;
import org.springframework.data.cassandra.repository.CassandraRepository;

public interface ArtistRepository extends CassandraRepository<Artist, String> {}
