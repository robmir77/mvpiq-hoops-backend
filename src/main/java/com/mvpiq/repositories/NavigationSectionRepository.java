package com.mvpiq.repositories;

import com.mvpiq.model.NavigationSection;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NavigationSectionRepository implements PanacheRepository<NavigationSection> {

    public Optional<NavigationSection> findBySectionKey(String sectionKey) {
        return find("sectionKey", sectionKey).firstResultOptional();
    }

    public List<NavigationSection> findByActive(Boolean active) {
        return find("active", active).list();
    }

    public List<NavigationSection> findAllActiveSorted() {
        return find("active = true order by sortOrder asc, title asc").list();
    }

    public boolean existsBySectionKey(String sectionKey) {
        return find("sectionKey", sectionKey).count() > 0;
    }

    public void deleteBySectionKey(String sectionKey) {
        delete("sectionKey", sectionKey);
    }

    public long countByActive(Boolean active) {
        return count("active", active);
    }

    public Optional<NavigationSection> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }
}
