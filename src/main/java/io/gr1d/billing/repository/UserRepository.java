package io.gr1d.billing.repository;

import io.gr1d.billing.model.User;
import io.gr1d.billing.model.enumerations.UserStatus;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByTenantRealmAndKeycloakId(String keycloakId, String tenantRealm);

    List<User> findByStatusAndPendingSyncTrue(UserStatus userStatus);

    @Modifying
    @Query("UPDATE User u SET u.status = 2, u.pendingSync = true WHERE u = :user")
    void blockUserPendingSync(@Param("user") User user);

    @Modifying
    @Query("UPDATE User u SET u.status = 1, u.pendingSync = true WHERE u = :user")
    void unblockUserPendingSync(@Param("user") User user);

    @Modifying
    @Query("UPDATE User u SET u.status = 2, u.pendingSync = false WHERE u = :user")
    void blockUser(@Param("user") User user);

    @Modifying
    @Query("UPDATE User u SET u.status = 1, u.pendingSync = false WHERE u = :user")
    void unblockUser(@Param("user") User user);


}
