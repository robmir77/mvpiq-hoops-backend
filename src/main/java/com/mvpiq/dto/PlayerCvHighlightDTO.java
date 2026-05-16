package com.mvpiq.dto;

import com.mvpiq.model.PlayerCvHighlight;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PlayerCvHighlightDTO {

    private UUID id;
    private String title;
    private String description;
    private String externalUrl;   // URL esterno (YouTube, Vimeo, ecc.)
    private String storageUrl;    // URL file caricato direttamente
    private String thumbnailUrl;
    private UUID mediaId;

    public static PlayerCvHighlightDTO fromEntity(PlayerCvHighlight h) {
        String externalUrl = null;
        String storageUrl  = null;
        String thumbnailUrl = null;
        UUID   mediaId     = null;

        externalUrl = h.getExternalUrl();
        if (h.getMedia() != null) {
            storageUrl   = h.getMedia().getStorageUrl();
            thumbnailUrl = h.getMedia().getThumbnailUrl();
            mediaId      = h.getMedia().getId();
        }

        return PlayerCvHighlightDTO.builder()
                .id(h.getId())
                .title(h.getTitle())
                .description(h.getDescription())
                .externalUrl(externalUrl)
                .storageUrl(storageUrl)
                .thumbnailUrl(thumbnailUrl)
                .mediaId(mediaId)
                .build();
    }
}
