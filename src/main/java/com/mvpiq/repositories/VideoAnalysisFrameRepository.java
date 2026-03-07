package com.mvpiq.repositories;

import com.mvpiq.model.VideoAnalysisFrame;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class VideoAnalysisFrameRepository
        implements PanacheRepositoryBase<VideoAnalysisFrame, UUID> {
}