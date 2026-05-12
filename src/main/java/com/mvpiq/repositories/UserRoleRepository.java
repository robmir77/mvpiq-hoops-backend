package com.mvpiq.repositories;

import com.mvpiq.model.UserRoleAssignment;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserRoleRepository implements PanacheRepositoryBase<UserRoleAssignment, UUID> {

    public List<UserRoleAssignment> findByUserId(UUID userId) {
        return list("user.id", userId);
    }

    public List<UserRoleAssignment> findByRoleId(UUID roleId) {
        return list("role.id", roleId);
    }

    public UserRoleAssignment findByUserIdAndRoleId(UUID userId, UUID roleId) {
        return find("user.id = ?1 and role.id = ?2", userId, roleId).firstResult();
    }

    public boolean existsByUserIdAndRoleId(UUID userId, UUID roleId) {
        return findByUserIdAndRoleId(userId, roleId) != null;
    }

    public void deleteByUserIdAndRoleId(UUID userId, UUID roleId) {
        UserRoleAssignment userRole = findByUserIdAndRoleId(userId, roleId);
        if (userRole != null) {
            delete(userRole);
        }
    }
}
