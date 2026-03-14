package com.mvpiq.service;

import com.mvpiq.model.VideoAnalysisResult;
import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.repositories.VideoAnalysisResultRepository;

import com.mvpiq.service.ai.ShotEvaluationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.File;
import java.util.List;

import jakarta.json.JsonObject;

@ApplicationScoped
public class VideoAnalysisAIService {

    @Inject
    VideoAnalysisResultRepository resultRepository;

    @Inject
    ShotEvaluationService shotAnalysisService;

    public void analyzeFrames(VideoAnalysisSession session, List<File> frames) {

        if (frames == null || frames.isEmpty()) {
            throw new RuntimeException("No frames extracted for analysis");
        }

        try {

            JsonObject aiResponse = shotAnalysisService.analyzeShot(frames);

            saveResult(session, aiResponse);

        } catch (Exception e) {

            session.status = "FAILED";
            throw new RuntimeException("AI analysis failed", e);

        }
    }

    /**
     * Salva il risultato nel DB come prima.
     */
    @Transactional
    public void saveResult(VideoAnalysisSession session, JsonObject aiResponse) {

        VideoAnalysisResult result = new VideoAnalysisResult();
        result.session = session;

        // Score
        if (aiResponse.containsKey("score")) {
            result.score = aiResponse.getInt("score");
        }

        // Posizioni della palla
        if (aiResponse.containsKey("positions")) {
            result.detectedErrors = aiResponse.getJsonArray("positions").toString(); // riuso campo come esempio
        }

        result.aiResponse = aiResponse.toString();

        resultRepository.persist(result);
    }
}