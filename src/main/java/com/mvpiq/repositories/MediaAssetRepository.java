package com.mvpiq.repositories;

import com.mvpiq.model.MediaAsset;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MediaAssetRepository implements PanacheRepositoryBase<MediaAsset, UUID> {

    public List<MediaAsset> findByOwner(UUID ownerId) {
        return list("ownerId", ownerId);
    }
}
