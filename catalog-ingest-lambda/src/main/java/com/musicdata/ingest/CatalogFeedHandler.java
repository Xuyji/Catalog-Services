package com.musicdata.ingest;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * AWS Lambda triggered by S3 {@code ObjectCreated} events under {@code incoming/}.
 *
 * <p>Each label delivery is validated and normalized, then written to:
 *
 * <ul>
 *   <li>{@code curated/catalog/ingest_date=YYYY-MM-DD/<file>.jsonl}: clean records for downstream
 *       Spark jobs and the catalog API loader;
 *   <li>{@code quarantine/<file>.errors.jsonl}: rejected lines with a reason, for the label-ops
 *       team.
 * </ul>
 *
 * <p>Output keys are derived from the input key, so an S3 event delivered twice (S3 events are
 * at-least-once) overwrites the same objects instead of duplicating them.
 */
public class CatalogFeedHandler implements RequestHandler<S3Event, CatalogFeedHandler.Summary> {

  public record Summary(int files, int validRecords, int rejectedRecords) {}

  private final S3Client s3;
  private final CatalogFeedProcessor processor = new CatalogFeedProcessor();

  /** Used by the Lambda runtime. Client is created once per container and reused across calls. */
  public CatalogFeedHandler() {
    this(S3Client.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build());
  }

  CatalogFeedHandler(S3Client s3) {
    this.s3 = s3;
  }

  @Override
  public Summary handleRequest(S3Event event, Context context) {
    LambdaLogger log = context.getLogger();
    int files = 0;
    int valid = 0;
    int rejected = 0;

    for (S3EventNotificationRecord record : event.getRecords()) {
      String bucket = record.getS3().getBucket().getName();
      String key = URLDecoder.decode(record.getS3().getObject().getKey(), StandardCharsets.UTF_8);
      if (!key.startsWith("incoming/")) {
        log.log("Skipping key outside incoming/: " + key);
        continue;
      }

      CatalogFeedProcessor.Result result;
      try (var in = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
        result = processor.process(new InputStreamReader(in, StandardCharsets.UTF_8), key);
      } catch (java.io.IOException e) {
        throw new UncheckedIOException(e);
      }

      String baseName = baseName(key);
      put(bucket, curatedKey(baseName, LocalDate.now(ZoneOffset.UTC)), toJsonLines(result.valid()));
      if (!result.rejected().isEmpty()) {
        put(bucket, "quarantine/" + baseName + ".errors.jsonl", toJsonLines(result.rejected()));
      }

      files++;
      valid += result.valid().size();
      rejected += result.rejected().size();
      log.log(String.format(
          "file=%s valid=%d rejected=%d", key, result.valid().size(), result.rejected().size()));
    }
    return new Summary(files, valid, rejected);
  }

  static String baseName(String key) {
    String name = key.substring(key.lastIndexOf('/') + 1);
    int dot = name.lastIndexOf('.');
    return dot > 0 ? name.substring(0, dot) : name;
  }

  static String curatedKey(String baseName, LocalDate ingestDate) {
    return "curated/catalog/ingest_date=" + ingestDate + "/" + baseName + ".jsonl";
  }

  static String toJsonLines(List<?> records) {
    return records.stream()
        .map(r -> {
          try {
            return CatalogFeedProcessor.MAPPER.writeValueAsString(r);
          } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
          }
        })
        .collect(Collectors.joining("\n"));
  }

  private void put(String bucket, String key, String body) {
    s3.putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/x-ndjson").build(),
        RequestBody.fromString(body, StandardCharsets.UTF_8));
  }
}
