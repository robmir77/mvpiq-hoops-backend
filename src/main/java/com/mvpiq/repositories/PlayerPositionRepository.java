package com.mvpiq.repositories;

import com.mvpiq.model.PlayerPosition;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class PlayerPositionRepository
        implements PanacheRepositoryBase<PlayerPosition, UUID> {
}