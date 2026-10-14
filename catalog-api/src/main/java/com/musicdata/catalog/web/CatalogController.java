package com.musicdata.catalog.web;

import com.musicdata.catalog.model.Artist;
import com.musicdata.catalog.model.Track;
import com.musicdata.catalog.model.TrackByArtist;
import com.musicdata.catalog.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class CatalogController {

  private final CatalogService catalog;

  public CatalogController(CatalogService catalog) {
    this.catalog = catalog;
  }

  @GetMapping("/tracks/{trackId}")
  public Track track(@PathVariable String trackId) {
    return catalog.getTrack(trackId).orElseThrow(() -> new NotFoundException("track", trackId));
  }

  @PutMapping("/tracks/{trackId}")
  public Track upsertTrack(@PathVariable String trackId, @Valid @RequestBody TrackRequest request) {
    return catalog.upsertTrack(request.toTrack(trackId));
  }

  @GetMapping("/artists/{artistId}")
  public Artist artist(@PathVariable String artistId) {
    return catalog.getArtist(artistId).orElseThrow(() -> new NotFoundException("artist", artistId));
  }

  @GetMapping("/artists/{artistId}/tracks")
  public List<TrackByArtist> artistTracks(
      @PathVariable String artistId,
      @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit) {
    return catalog.tracksByArtist(artistId, limit);
  }

  @ExceptionHandler(NotFoundException.class)
  ProblemDetail notFound(NotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  static class NotFoundException extends RuntimeException {
    NotFoundException(String kind, String id) {
      super(kind + " " + id + " not found");
    }
  }
}
