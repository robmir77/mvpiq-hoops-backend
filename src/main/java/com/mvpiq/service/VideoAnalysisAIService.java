package com.mvpiq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvpiq.model.VideoAnalysisResult;
import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.repositories.VideoAnalysisResultRepository;
import io.vertx.core.json.JsonObject;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.List;

@ApplicationScoped
public class VideoAnalysisAIService {

    @Inject
    VideoAnalysisResultRepository resultRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonObject analyze(File videoFile) {

        try {

            String boundary = "----Boundary" + System.currentTimeMillis();

            byte[] fileBytes = Files.readAllBytes(videoFile.toPath());

            String bodyStart =
                    "--" + boundary + "\r\n" +
                            "Content-Disposition: form-data; name=\"video\"; filename=\"" + videoFile.getName() + "\"\r\n" +
                            "Content-Type: video/mp4\r\n\r\n";

            String bodyEnd = "\r\n--" + boundary + "--";

            byte[] body = concat(
                    bodyStart.getBytes(),
                    fileBytes,
                    bodyEnd.getBytes()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8001/analyze"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return new JsonObject(response.body());

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("AI service call failed", e);
        }
    }

    private byte[] concat(byte[]... arrays) {

        int totalLength = 0;

        for (byte[] arr : arrays) {
            totalLength += arr.length;
        }

        byte[] result = new byte[totalLength];

        int offset = 0;

        for (byte[] arr : arrays) {
            System.arraycopy(arr, 0, result, offset, arr.length);
            offset += arr.length;
        }

        return result;
    }

    public void analyzeFrames(VideoAnalysisSession session, List<File> frames) {

        if (frames == null || frames.isEmpty()) {
            throw new RuntimeException("No frames extracted for analysis");
        }

        try {

            // per ora usiamo il primo frame (semplice)
            File frame = frames.get(0);

            JsonObject aiResponse = analyze(frame);

            saveResult(session, aiResponse);

        } catch (Exception e) {

            session.status = "FAILED";
            throw new RuntimeException("AI analysis failed", e);

        }
    }

    @Transactional
    public void saveResult(VideoAnalysisSession session, JsonObject aiResponse) {

        VideoAnalysisResult result = new VideoAnalysisResult();

        result.session = session;

        if (aiResponse.containsKey("score")) {
            result.score = aiResponse.getInteger("score");
        }

        if (aiResponse.containsKey("errors")) {
            result.detectedErrors = aiResponse.getJsonArray("errors").encode();
        }

        if (aiResponse.containsKey("suggestions")) {
            result.suggestions = aiResponse.getJsonArray("suggestions").encode();
        }

        result.aiResponse = aiResponse.encode();

        resultRepository.persist(result);
    }
}