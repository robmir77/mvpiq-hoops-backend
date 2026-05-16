package com.mvpiq.service;

import com.mvpiq.enums.UserRole;
import com.mvpiq.model.NavigationSection;
import com.mvpiq.repositories.RolePermissionRepository;
import com.mvpiq.security.RoleBasedSecurityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class NavigationService {

    @Inject
    RolePermissionRepository permissionRepository;

    @Inject
    RoleBasedSecurityService securityService;

    /**
     * Restituisce le sezioni accessibili per l'utente corrente dal database
     */
    public List<NavigationItem> getAccessibleSections() {
        log.info("Getting accessible sections for current user from database");
        
        boolean isAnonymous = securityService.isAnonymous();
        log.info("User is anonymous: {}", isAnonymous);
        
        if (isAnonymous) {
            log.info("User is anonymous - returning empty list");
            return Collections.emptyList();
        }

        Set<NavigationSection> accessibleSections = new HashSet<>();
        
        // Aggiungi sezioni specifiche per ruolo
        for (UserRole role : UserRole.values()) {
            if (hasRole(role)) {
                List<NavigationSection> roleSections = permissionRepository.findAccessibleSectionsByRole(role);
                log.info("Found {} sections for role {}", roleSections.size(), role);
                accessibleSections.addAll(roleSections);
            }
        }
        
        log.info("Total accessible sections: {}", accessibleSections.size());
        
        return accessibleSections.stream()
                .sorted(Comparator.comparing(NavigationSection::getSortOrder).thenComparing(NavigationSection::getTitle))
                .map(this::createNavigationItem)
                .collect(Collectors.toList());
    }

    /**
     * Metodo helper per verificare i ruoli in modo sicuro
     */
    private boolean hasRole(UserRole role) {
        try {
            boolean result = securityService.hasRole(role);
            //log.info("Checking role {}: {}", role, result);
            return result;
        } catch (Exception e) {
            log.warn("Error checking role {}: {}", role, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se l'utente corrente può accedere a una specifica sezione
     */
    public boolean canAccessSection(String sectionKey) {
        if (securityService.isAnonymous()) {
            return false;
        }
        
        // Admin può accedere a tutto
        if (hasRole(UserRole.ADMIN)) {
            return true;
        }
        
        // Controlla se la sezione è accessibile per uno dei ruoli dell'utente
        for (UserRole role : UserRole.values()) {
            if (hasRole(role)) {
                List<NavigationSection> roleSections = permissionRepository.findAccessibleSectionsByRole(role);
                boolean hasAccess = roleSections.stream()
                        .anyMatch(section -> section.getSectionKey().equals(sectionKey));
                if (hasAccess) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Restituisce tutte le sezioni disponibili (per admin/debug)
     */
    public List<NavigationItem> getAllSections() {
        List<NavigationSection> allSections = permissionRepository.findAll().stream()
                .map(rp -> rp.getSection())
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(NavigationSection::getSortOrder).thenComparing(NavigationSection::getTitle))
                .collect(Collectors.toList());
        
        return allSections.stream()
                .map(this::createNavigationItem)
                .collect(Collectors.toList());
    }

    private NavigationItem createNavigationItem(NavigationSection section) {
        return NavigationItem.builder()
                .id(section.getSectionKey())
                .title(section.getTitle())
                .description(section.getDescription())
                .icon(section.getIcon())
                .iconColor(section.getIconColor())
                .accessible(canAccessSection(section.getSectionKey()))
                .sortOrder(section.getSortOrder())
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class NavigationItem {
        private String id;
        private String title;
        private String description;
        private String icon;
        private String iconColor;
        private Boolean accessible;
        private Integer sortOrder;
    }
}
