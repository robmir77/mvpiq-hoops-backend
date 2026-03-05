package com.mvpiq.service;

import com.mvpiq.dto.MediaAssetDTO;
import com.mvpiq.dto.MediaUploadRequest;
import com.mvpiq.model.MediaAsset;
import com.mvpiq.repositories.MediaAssetRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MediaService {

    @Inject
    MediaAssetRepository mediaRepo;

    public List<MediaAssetDTO> getVideosByAthlete(UUID athleteId) {
        return mediaRepo.findByOwner(athleteId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public MediaAssetDTO upload(MediaUploadRequest req) {

        MediaAsset entity = MediaAsset.builder()
                .id(null)
                .ownerId(UUID.fromString(req.getOwnerId()))
                .title(req.getTitle())
                .description(req.getDescription())
                .mediaType(req.getMediaType())
                .storageUrl(req.getStorageUrl())
                .thumbnailUrl(req.getThumbnailUrl())
                .durationSeconds(req.getDurationSeconds())
                .width(req.getWidth())
                .height(req.getHeight())
                .isOfficial(req.getIsOfficial())
                .createdAt(OffsetDateTime.now())
                .build();

        mediaRepo.persist(entity);

        return toDto(entity);
    }

    private MediaAssetDTO toDto(MediaAsset m) {
        return MediaAssetDTO.builder()
                .id(m.getId())
                .title(m.getTitle())
                .description(m.getDescription())
                .mediaType(m.getMediaType())
                .storageUrl(m.getStorageUrl())
                .thumbnailUrl(m.getThumbnailUrl())
                .durationSeconds(m.getDurationSeconds())
                .width(m.getWidth())
                .height(m.getHeight())
                .isOfficial(m.getIsOfficial())
                .build();
    }
}
