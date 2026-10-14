package com.musicdata.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.musicdata.catalog.model.Track;
import com.musicdata.catalog.service.CatalogService;
import com.musicdata.catalog.web.CatalogController;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

  @Autowired MockMvc mvc;
  @MockBean CatalogService catalog;

  private static final Track TRACK =
      new Track("t000001", "USRC17607839", "Song", "a00001", null, 210_000,
          LocalDate.parse("2026-09-01"), Set.of("pop"), false);

  @Test
  void returnsTrack() throws Exception {
    when(catalog.getTrack("t000001")).thenReturn(Optional.of(TRACK));
    mvc.perform(get("/v1/tracks/t000001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isrc").value("USRC17607839"));
  }

  @Test
  void missingTrackIs404ProblemDetail() throws Exception {
    when(catalog.getTrack("nope")).thenReturn(Optional.empty());
    mvc.perform(get("/v1/tracks/nope"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("track nope not found"));
  }

  @Test
  void upsertNormalizesGenres() throws Exception {
    when(catalog.upsertTrack(any())).thenAnswer(inv -> inv.getArgument(0));
    String body =
        """
        {"isrc":"USRC17607839","title":"  Song  ","artistId":"a00001","durationMs":210000,
         "releaseDate":"2026-09-01","genres":["Pop"," INDIE "]}
        """;
    mvc.perform(put("/v1/tracks/t000001").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Song"))
        .andExpect(jsonPath("$.genres.length()").value(2));
  }

  @Test
  void invalidIsrcIsRejectedBeforeReachingTheService() throws Exception {
    String body =
        """
        {"isrc":"not-an-isrc","title":"Song","artistId":"a00001","durationMs":210000,
         "releaseDate":"2026-09-01"}
        """;
    mvc.perform(put("/v1/tracks/t000001").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    verify(catalog, never()).upsertTrack(any());
  }

  @Test
  void artistTracksLimitIsBounded() throws Exception {
    mvc.perform(get("/v1/artists/a00001/tracks").param("limit", "10000"))
        .andExpect(status().isBadRequest());
  }
}
