package com.mvpiq.service;

import com.mvpiq.model.Player;
import com.mvpiq.model.PlayerPosition;
import com.mvpiq.model.PositionMetadata;
import com.mvpiq.repositories.PlayerPositionRepository;
import com.mvpiq.repositories.PositionMetadataRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
@ApplicationScoped
public class PlayerPositionService {

    @Inject
    PlayerPositionRepository playerPositionRepository;

    @Inject
    PositionMetadataRepository positionMetadataRepository;

    @Transactional
    public void updatePositions(Player player,
                                UUID mainId,
                                List<UUID> secondaryIds) {

        // 🔹 Normalizziamo input
        if (secondaryIds == null) {
            secondaryIds = new ArrayList<>();
        }

        // 🔹 Rimuoviamo duplicati
        Set<UUID> uniqueIds = new HashSet<>(secondaryIds);

        if (mainId != null) {
            uniqueIds.remove(mainId); // evita doppio inserimento
        }

        // 🔥 1️⃣ Cancella vecchie relazioni
        playerPositionRepository.delete("player = ?1", player);

        // 🔥 2️⃣ Se non c'è nulla → stop
        if (mainId == null && uniqueIds.isEmpty()) {
            return;
        }

        // 🔥 3️⃣ Carichiamo tutte le metadata in una sola query
        Set<UUID> allIds = new HashSet<>(uniqueIds);
        if (mainId != null) {
            allIds.add(mainId);
        }

        List<PositionMetadata> metadataList =
                positionMetadataRepository.list("id in ?1", allIds);

        Map<UUID, PositionMetadata> metadataMap = new HashMap<>();
        for (PositionMetadata m : metadataList) {
            metadataMap.put(m.getId(), m);
        }

        // 🔥 4️⃣ Validazione ID
        for (UUID id : allIds) {
            if (!metadataMap.containsKey(id)) {
                throw new IllegalArgumentException("Invalid position id: " + id);
            }
        }

        // 🔥 5️⃣ Inseriamo primary
        if (mainId != null) {

            PlayerPosition primary = PlayerPosition.builder()
                    .player(player)
                    .position(metadataMap.get(mainId))
                    .isPrimary(true)
                    .createdAt(OffsetDateTime.now())
                    .build();

            playerPositionRepository.persist(primary);
        }

        // 🔥 6️⃣ Inseriamo secondary
        for (UUID id : uniqueIds) {

            PlayerPosition secondary = PlayerPosition.builder()
                    .player(player)
                    .position(metadataMap.get(id))
                    .isPrimary(false)
                    .createdAt(OffsetDateTime.now())
                    .build();

            playerPositionRepository.persist(secondary);
        }
    }
}