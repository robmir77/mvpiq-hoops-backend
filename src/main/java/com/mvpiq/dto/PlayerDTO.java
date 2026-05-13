package com.mvpiq.dto;

import com.mvpiq.model.Player;
import com.mvpiq.model.PlayerPosition;
import com.mvpiq.model.PositionMetadata;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerDTO {

    // ID coincide con users.id
    private UUID id;

    // Campi base User
    private String username;
    private String displayName;
    private String avatarUrl;
    private Boolean verified;
    private Boolean publicProfile;
    private String bio;

    // Campi specifici Player
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    private Short heightCm;
    private Short weightKg;

    // 🔥 ORA USIAMO GLI ID
    private UUID mainPositionId;
    private List<UUID> secondaryPositionIds;

    private String mainPositionLabel;
    private List<String> secondaryPositionLabels;

    private String level;
    private String dominantHand;
    private String country;
    private String city;
    private Integer approximateAge;
    private String gender;

    public static PlayerDTO fromEntity(Player p) {

        UUID mainId = null;
        String mainLabel = null;
        List<UUID> secondaryIds = new ArrayList<>();
        List<String> secondaryLabels = new ArrayList<>();

        for (PlayerPosition pp : p.getPositions()) {

            PositionMetadata meta = pp.getPosition();

            if (Boolean.TRUE.equals(pp.getIsPrimary())) {
                mainId = meta.getId();
                mainLabel = meta.getLabel();
            } else {
                secondaryIds.add(meta.getId());
                secondaryLabels.add(meta.getLabel());
            }
        }

        return PlayerDTO.builder()
                .id(p.getId())

                // Campi User
                .username(p.getUsername())
                .displayName(p.getDisplayName())
                .avatarUrl(p.getAvatarUrl())
                .verified(p.getVerified())
                .publicProfile(p.getPublicProfile())
                .bio(p.getBio())

                // Campi Player
                .birthDate(p.getBirthDate())
                .heightCm(p.getHeightCm())
                .weightKg(p.getWeightKg())

                .mainPositionId(mainId)
                .mainPositionLabel(mainLabel)
                .secondaryPositionIds(secondaryIds)
                .secondaryPositionLabels(secondaryLabels)

                .level(p.getLevel())
                .dominantHand(p.getDominantHand())
                .country(p.getCountry())
                .city(p.getCity())
                .approximateAge(p.getApproximateAge())
                .gender(p.getGender())
                .build();
    }

    public void updateEntity(Player p) {

        // Campi User modificabili - solo se non null
        if (displayName != null) p.setDisplayName(displayName);
        if (avatarUrl != null) p.setAvatarUrl(avatarUrl);
        if (publicProfile != null) p.setPublicProfile(publicProfile);
        if (bio != null) p.setBio(bio);

        // Campi Player - solo se non null
        if (birthDate != null) p.setBirthDate(birthDate);
        if (heightCm != null) p.setHeightCm(heightCm);
        if (weightKg != null) p.setWeightKg(weightKg);
        if (level != null) p.setLevel(level);
        if (dominantHand != null) p.setDominantHand(dominantHand);
        if (country != null) p.setCountry(country);
        if (city != null) p.setCity(city);
        if (gender != null) p.setGender(gender);

        // 🔥 Le posizioni si aggiornano nel PlayerPositionService
    }

    public static PlayerDTO toDTO(Player p) {
        return fromEntity(p);
    }
}