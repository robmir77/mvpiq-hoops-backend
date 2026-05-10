package com.mvpiq.repositories;

import com.mvpiq.model.NavigationSection;
import com.mvpiq.model.RolePermission;
import com.mvpiq.enums.UserRole;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class RolePermissionRepository implements PanacheRepository<RolePermission> {

    public Optional<RolePermission> findByRoleAndSection(UserRole role, NavigationSection section) {
        return find("role = ?1 and section = ?2", role.name(), section).firstResultOptional();
    }

    public List<RolePermission> findByRole(UserRole role) {
        return find("role", role.name()).list();
    }

    public List<RolePermission> findByRoleAndCanAccess(UserRole role, Boolean canAccess) {
        return find("role = ?1 and canAccess = ?2", role.name(), canAccess).list();
    }

    public List<NavigationSection> findAccessibleSectionsByRole(UserRole role) {
        List<RolePermission> permissions = find("role = ?1 and canAccess = true", role.name()).list();
        return permissions.stream()
                .map(RolePermission::getSection)
                .filter(section -> section != null && section.getActive())
                .sorted(Comparator.comparing(NavigationSection::getSortOrder).thenComparing(NavigationSection::getTitle))
                .collect(Collectors.toList());
    }

    public void deleteByRoleAndSection(UserRole role, NavigationSection section) {
        delete("role = ?1 and section = ?2", role.name(), section);
    }

    public boolean existsByRoleAndSection(UserRole role, NavigationSection section) {
        return find("role = ?1 and section = ?2", role.name(), section).count() > 0;
    }

    public long countByRole(UserRole role) {
        return count("role", role.name());
    }
}
