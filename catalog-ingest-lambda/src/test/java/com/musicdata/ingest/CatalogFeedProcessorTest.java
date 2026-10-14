package com.musicdata.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.musicdata.ingest.CatalogFeedProcessor.NormalizedTrack;
import com.musicdata.ingest.CatalogFeedProcessor.Rejected;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CatalogFeedProcessorTest {

  private static CatalogFeedProcessor.Result result;

  @BeforeAll
  static void processSampleDelivery() {
    var in = CatalogFeedProcessorTest.class.getResourceAsStream("/label-delivery.jsonl");
    result =
        new CatalogFeedProcessor()
            .process(new InputStreamReader(in, StandardCharsets.UTF_8), "incoming/label-delivery.jsonl");
  }

  @Test
  void keepsValidRecordsAndDedupesByIsrcLastWins() {
    assertEquals(2, result.valid().size());
    NormalizedTrack remaster = result.valid().get(1);
    assertEquals("GBAYE0601498", remaster.isrc());
    assertEquals("Second Song (Remastered)", remaster.title());
    assertTrue(remaster.explicit());
  }

  @Test
  void normalizesIsrcTitleAndGenres() {
    NormalizedTrack first = result.valid().get(0);
    assertEquals("USRC17607839", first.isrc());
    assertEquals("t-usrc17607839", first.trackId());
    assertEquals("First Song", first.title());
    assertEquals(List.of("indie", "pop"), first.genres());
  }

  @Test
  void rejectsInvalidLinesWithReasonsAndLineNumbers() {
    List<Rejected> rejected = result.rejected();
    assertEquals(5, rejected.size());
    assertEquals(3, rejected.get(0).lineNumber());
    assertTrue(rejected.get(0).reason().startsWith("invalid ISRC"));
    assertEquals("missing title", rejected.get(1).reason());
    assertTrue(rejected.get(2).reason().startsWith("duration_ms out of range"));
    assertTrue(rejected.get(3).reason().startsWith("invalid release_date"));
    assertEquals("malformed JSON", rejected.get(4).reason());
  }

  @Test
  void outputKeysAreDeterministicSoRetriesOverwrite() {
    assertEquals("label-delivery", CatalogFeedHandler.baseName("incoming/2026/label-delivery.jsonl"));
    assertEquals(
        "curated/catalog/ingest_date=2026-10-13/label-delivery.jsonl",
        CatalogFeedHandler.curatedKey("label-delivery", LocalDate.parse("2026-10-13")));
  }

  @Test
  void writesJsonLines() {
    String out = CatalogFeedHandler.toJsonLines(result.valid());
    assertEquals(2, out.split("\n").length);
    assertTrue(out.contains("\"trackId\":\"t-usrc17607839\""));
  }
}
