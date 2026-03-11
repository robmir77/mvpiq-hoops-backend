package com.mvpiq.service;

import com.mvpiq.model.VideoAnalysisResult;
import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.repositories.VideoAnalysisResultRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.File;
import java.util.List;

import org.bytedeco.opencv.opencv_core.Point;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

@ApplicationScoped
public class VideoAnalysisAIService {

    @Inject
    VideoAnalysisResultRepository resultRepository;

    @Inject
    BasketballShotAnalysisService basketballShotAnalysisService;

    /**
     * Analizza i frame usando il servizio Java locale per rilevare la palla e il canestro.
     */
    public void analyzeFrames(VideoAnalysisSession session, List<File> frames) {
        if (frames == null || frames.isEmpty()) {
            throw new RuntimeException("No frames extracted for analysis");
        }

        try {
            // Analizza i frame e ottieni le posizioni della palla
            List<Point> ballPositions = basketballShotAnalysisService.analyzeShot(frames);

            // Converti i punti in JSON
            JsonArrayBuilder positionsArray = Json.createArrayBuilder();
            for (Point p : ballPositions) {
                positionsArray.add(Json.createObjectBuilder()
                        .add("x", p.x())
                        .add("y", p.y()));
            }

            // Crea JsonObject simulando il vecchio formato AI
            JsonObjectBuilder aiResponse = Json.createObjectBuilder()
                    .add("score", ballPositions.size()) // esempio di punteggio: numero di frame validi
                    .add("positions", positionsArray);

            saveResult(session, aiResponse.build());

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