package com.mvpiq.service;

import com.mvpiq.model.VideoAnalysisResult;
import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.repositories.VideoAnalysisResultRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class VideoAnalysisAIService {

    @Inject
    VideoAnalysisResultRepository resultRepository;

    public void analyzeFrames(VideoAnalysisSession session, List<File> frames) {

        int score = 85;

        String errors = """
        [{"code":"ELBOW_OUT","severity":"medium"}]
        """;

        String suggestions = """
        [{"text":"Keep elbow aligned with the rim"}]
        """;

        String aiResponse = """
        {"comment":"Good shot but elbow slightly open"}
        """;

        VideoAnalysisResult result = new VideoAnalysisResult();
        result.session = session;
        result.score = score;
        result.detectedErrors = errors;
        result.suggestions = suggestions;
        result.aiResponse = aiResponse;
        result.createdAt = OffsetDateTime.now();

        resultRepository.persist(result);
    }
}