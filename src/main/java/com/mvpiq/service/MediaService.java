package com.mvpiq.service;

import com.mvpiq.dto.MediaAssetDTO;
import com.mvpiq.model.MediaAsset;
import com.mvpiq.enums.MediaType;
import com.mvpiq.repositories.MediaAssetRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MediaService {

    @Inject
    MediaAssetRepository mediaRepo;

    @Inject
    SupabaseStorageService storageService;

    public List<MediaAssetDTO> getVideosByAthlete(UUID athleteId) {

        return mediaRepo.findByOwner(athleteId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public MediaAssetDTO uploadVideo(File file, UUID userId) {

        String extension = file.getName()
                .substring(file.getName().lastIndexOf("."));

        String path = userId + "/" + UUID.randomUUID() + extension;

        String publicUrl = storageService.uploadVideo(file, path);

        MediaAsset entity = MediaAsset.builder()
                .ownerId(userId)
                .storageUrl(publicUrl)
                .mediaType(MediaType.user_upload.toString())
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