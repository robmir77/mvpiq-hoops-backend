package com.mvpiq.repositories;

import com.mvpiq.model.PositionMetadata;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class PositionMetadataRepository
        implements PanacheRepositoryBase<PositionMetadata, UUID> {
}