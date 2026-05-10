package com.mvpiq.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvpiq.model.ChecklistTemplateItemOption;
import com.mvpiq.model.PlayerPosition;
import com.mvpiq.model.PositionMetadata;
import com.mvpiq.repositories.PlayerPositionRepository;
import com.mvpiq.repositories.PositionMetadataRepository;
import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class ChecklistDynamicOptionsService {

    @Inject
    PositionMetadataRepository positionMetadataRepository;

    @Inject
    PlayerPositionRepository playerPositionRepository;

    @Inject
    EntityManager entityManager;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Recupera le opzioni dinamiche basate sul select_source
     * @param selectSource - STATIC | POSITION_METADATA | PLAYER_POSITION | TRAINING_TYPE | SQL
     * @param selectQuery - Query SQL da eseguire quando select_source = 'SQL'
     * @return lista di opzioni dinamiche
     */
    @Transactional
    public List<ChecklistTemplateItemOption> getDynamicOptions(String selectSource, String selectQuery) {
        log.info("Getting dynamic options for selectSource: {}", selectSource);

        if (selectSource == null || selectSource.isBlank()) {
            log.warn("selectSource is null or blank");
            return Collections.emptyList();
        }

        return switch (selectSource.toUpperCase()) {
            case "STATIC" -> {
                log.warn("STATIC should use static options from template");
                yield Collections.emptyList();
            }
            case "POSITION_METADATA" -> getPositionMetadataOptions();
            case "PLAYER_POSITION" -> getPlayerPositionOptions();
            case "TRAINING_TYPE" -> getTrainingTypeOptions();
            case "SQL" -> getSqlQueryOptions(selectQuery);
            default -> {
                log.warn("Unknown selectSource: {}", selectSource);
                yield Collections.emptyList();
            }
        };
    }

    /**
     * Recupera le opzioni da position_metadata
     */
    private List<ChecklistTemplateItemOption> getPositionMetadataOptions() {
        log.info("Fetching position metadata options");
        List<PositionMetadata> positions = positionMetadataRepository.listAll()
                .stream()
                .filter(p -> p.getIsActive() != null && p.getIsActive())
                .sorted(Comparator.comparing(PositionMetadata::getSortOrder))
                .toList();

        return positions.stream()
                .map(pos -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("category", pos.getCategory());
                    metadata.put("description", pos.getDescription());
                    try {
                        String metadataJson = objectMapper.writeValueAsString(metadata);
                        return ChecklistTemplateItemOption.builder()
                                .valueCode(pos.getCode())
                                .valueLabel(pos.getLabel())
                                .sortOrder(pos.getSortOrder())
                                .metadata(metadataJson)
                                .build();
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing metadata for position: {}", pos.getCode(), e);
                        return ChecklistTemplateItemOption.builder()
                                .valueCode(pos.getCode())
                                .valueLabel(pos.getLabel())
                                .sortOrder(pos.getSortOrder())
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Recupera le opzioni da player_position (posizioni uniche dei giocatori)
     */
    private List<ChecklistTemplateItemOption> getPlayerPositionOptions() {
        log.info("Fetching player position options");
        // Recupera le posizioni uniche dai profili giocatori
        Set<PositionMetadata> uniquePositions = playerPositionRepository.listAll()
                .stream()
                .map(PlayerPosition::getPosition)
                .filter(Objects::nonNull)
                .filter(p -> p.getIsActive() != null && p.getIsActive())
                .collect(Collectors.toSet());

        return uniquePositions.stream()
                .sorted(Comparator.comparing(PositionMetadata::getSortOrder))
                .map(pos -> ChecklistTemplateItemOption.builder()
                        .valueCode(pos.getCode())
                        .valueLabel(pos.getLabel())
                        .sortOrder(pos.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Recupera le opzioni per training_type (MATCH, TRAINING)
     */
    private List<ChecklistTemplateItemOption> getTrainingTypeOptions() {
        log.info("Fetching training type options");
        return List.of(
                ChecklistTemplateItemOption.builder()
                        .valueCode("MATCH")
                        .valueLabel("Match")
                        .sortOrder(1)
                        .build(),
                ChecklistTemplateItemOption.builder()
                        .valueCode("TRAINING")
                        .valueLabel("Allenamento")
                        .sortOrder(2)
                        .build()
        );
    }

    /**
     * Esegue una query SQL personalizzata per recuperare le opzioni
     * La query deve restituire almeno una colonna con il valore/label delle opzioni
     * Formato atteso: SELECT value_code, value_label, sort_order FROM ...
     * Se sort_order non è presente, usa l'ordine della query
     * Le colonne aggiuntive oltre alle prime 3 vengono incluse nel metadata
     */
    private List<ChecklistTemplateItemOption> getSqlQueryOptions(String selectQuery) {
        log.info("Executing SQL query for dynamic options: {}", selectQuery);

        if (selectQuery == null || selectQuery.isBlank()) {
            log.warn("SQL query is null or blank");
            return Collections.emptyList();
        }

        try {
            // Esegue la query SQL nativa
            @SuppressWarnings("unchecked")
            List<Object[]> results = entityManager.createNativeQuery(selectQuery).getResultList();

            log.info("SQL query returned {} rows", results.size());
            
            List<ChecklistTemplateItemOption> options = results.stream()
                    .map(row -> {
                        // La query deve restituire almeno value_code e value_label
                        // Opzionalmente può includere sort_order
                        String valueCode = row.length > 0 ? String.valueOf(row[0]) : null;
                        String valueLabel = row.length > 1 ? String.valueOf(row[1]) : valueCode;
                        Integer sortOrder = row.length > 2 && row[2] != null ?
                                Integer.valueOf(String.valueOf(row[2])) : 0;

                        // Colonne aggiuntive (dalla 4a in poi) vanno nel metadata
                        Map<String, Object> metadata = new HashMap<>();
                        if (row.length > 3) {
                            // Per semplicità, assumiamo che le colonne aggiuntive abbiano nomi generici
                            // In produzione, potresti voler usare ResultSetMetaData per ottenere i nomi reali
                            for (int i = 3; i < row.length; i++) {
                                if (row[i] != null) {
                                    metadata.put("column_" + i, row[i]);
                                }
                            }
                        }

                        try {
                            String metadataJson = metadata.isEmpty() ? null : objectMapper.writeValueAsString(metadata);
                            ChecklistTemplateItemOption option = ChecklistTemplateItemOption.builder()
                                    .valueCode(valueCode)
                                    .valueLabel(valueLabel)
                                    .sortOrder(sortOrder)
                                    .metadata(metadataJson)
                                    .build();
                            
                            log.debug("Created option: valueCode={}, valueLabel={}, sortOrder={}, metadata={}", 
                                    valueCode, valueLabel, sortOrder, metadataJson);
                            
                            return option;
                        } catch (JsonProcessingException e) {
                            log.error("Error serializing metadata for SQL option: {}", valueCode, e);
                            return ChecklistTemplateItemOption.builder()
                                    .valueCode(valueCode)
                                    .valueLabel(valueLabel)
                                    .sortOrder(sortOrder)
                                    .build();
                        }
                    })
                    .filter(option -> option.getValueCode() != null)
                    .collect(Collectors.toList());
            
            log.info("Processed {} options from SQL query", options.size());
            return options;
        } catch (Exception e) {
            log.error("Error executing SQL query for dynamic options: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
