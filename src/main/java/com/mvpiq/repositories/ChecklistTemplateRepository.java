package com.mvpiq.repositories;

import com.mvpiq.model.ChecklistTemplate;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class ChecklistTemplateRepository implements PanacheRepository<ChecklistTemplate> {

    /**
     * Recupera tutti i template attivi per un determinato entryType
     * con caricamento eager degli item.
     */
    public List<ChecklistTemplate> findActiveByType(String entryType) {
        log.info("Finding active templates for entryType: {}", entryType);

        List<ChecklistTemplate> templates = getEntityManager()
                .createQuery("""
                select distinct t
                from ChecklistTemplate t
                left join fetch t.items i
                left join fetch i.options o
                where t.entryType = :type
                  and t.isActive = true
                order by t.code
            """, ChecklistTemplate.class)
                .setParameter("type", entryType)
                .getResultList();

        log.info("Found {} templates for entryType: {}", templates.size(), entryType);
        for (ChecklistTemplate template : templates) {
            log.info("Template: {} ({}) - Items: {}", template.getName(), template.getCode(), 
                    template.getItems() != null ? template.getItems().size() : 0);
        }

        return templates;
    }

    /**
     * Recupera un template per codice con item caricati.
     */
    public Optional<ChecklistTemplate> findByCodeWithItems(String code) {

        return getEntityManager()
                .createQuery("""
                    select distinct t
                    from ChecklistTemplate t
                    left join fetch t.items i
                    where t.code = :code
                """, ChecklistTemplate.class)
                .setParameter("code", code)
                .getResultStream()
                .findFirst();
    }

    /**
     * Recupera solo template attivi (senza fetch).
     */
    public List<ChecklistTemplate> findAllActive() {
        return list("isActive = true order by entryType, code");
    }

    /**
     * Recupera un template per ID UUID.
     */
    public ChecklistTemplate findById(UUID id) {
        return find("id", id).firstResult();
    }

    /**
     * Cancella un template per ID UUID.
     */
    public boolean deleteById(UUID id) {
        ChecklistTemplate template = findById(id);
        if (template == null) {
            return false;
        }
        delete(template);
        return true;
    }
}
