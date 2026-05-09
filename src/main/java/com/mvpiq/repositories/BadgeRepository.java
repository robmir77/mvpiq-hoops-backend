package com.mvpiq.repositories;

import com.mvpiq.model.Badge;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class BadgeRepository implements PanacheRepositoryBase<Badge, UUID> {

    public List<Badge> findByCategory(String category) {
        if (category == null || category.isEmpty()) {
            return findAll().list();
        }
        return list("category", category);
    }

    public Optional<Badge> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }

    public boolean existsByCode(String code) {
        return count("code", code) > 0;
    }

    public List<Badge> findAllActive() {
        return list("isActive", true);
    }
}
