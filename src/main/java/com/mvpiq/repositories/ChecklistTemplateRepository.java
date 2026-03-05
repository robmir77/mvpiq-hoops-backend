package com.mvpiq.repositories;

import com.mvpiq.model.ChecklistTemplate;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChecklistTemplateRepository implements PanacheRepository<ChecklistTemplate> {

    /**
     * Recupera tutti i template attivi per un determinato entryType
     * con caricamento eager degli item.
     */
    public List<ChecklistTemplate> findActiveByType(String entryType) {

        return getEntityManager()
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
}
