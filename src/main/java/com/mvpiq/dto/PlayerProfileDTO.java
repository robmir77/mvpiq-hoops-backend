package com.mvpiq.dto;

import com.mvpiq.model.Player;
import com.mvpiq.model.PlayerPosition;
import com.mvpiq.model.PositionMetadata;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerProfileDTO {

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

    public static PlayerProfileDTO fromEntity(Player p) {

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

        return PlayerProfileDTO.builder()
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

        // Campi User modificabili
        p.setDisplayName(displayName);
        p.setAvatarUrl(avatarUrl);
        p.setPublicProfile(publicProfile);
        p.setBio(bio);

        // Campi Player
        p.setBirthDate(birthDate);
        p.setHeightCm(heightCm);
        p.setWeightKg(weightKg);
        p.setLevel(level);
        p.setDominantHand(dominantHand);
        p.setCountry(country);
        p.setCity(city);
        p.setGender(gender);

        // 🔥 Le posizioni si aggiornano nel PlayerPositionService
    }

    public static PlayerProfileDTO toDTO(Player p) {
        return fromEntity(p);
    }
}