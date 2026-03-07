package com.mvpiq.repositories;

import com.mvpiq.model.VideoAnalysisResult;
import com.mvpiq.model.VideoAnalysisSession;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class VideoAnalysisResultRepository implements PanacheRepository<VideoAnalysisResult> {

    @Transactional
    public void saveResult(VideoAnalysisSession session,
                           int score,
                           String detectedErrors,
                           String suggestions,
                           String aiResponse) {

        VideoAnalysisResult result = new VideoAnalysisResult();

        result.session = session;
        result.score = score;
        result.detectedErrors = detectedErrors;
        result.suggestions = suggestions;
        result.aiResponse = aiResponse;

        persist(result);
    }
}
