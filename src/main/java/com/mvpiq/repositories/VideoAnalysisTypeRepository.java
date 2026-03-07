package com.mvpiq.repositories;

import com.mvpiq.model.VideoAnalysisType;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VideoAnalysisTypeRepository implements PanacheRepository<VideoAnalysisType> {

    public List<VideoAnalysisType> findActive() {
        return list("isActive", true);
    }

    public Optional<VideoAnalysisType> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }
}
