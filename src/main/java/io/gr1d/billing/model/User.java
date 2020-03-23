package io.gr1d.billing.model;

import io.gr1d.billing.model.enumerations.UserStatus;
import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * @author Raúl Sola
 */
@Entity
@Getter @Setter
@Table(name = "user")
@EntityListeners(AuditListener.class)
public class User extends BaseModel {

    @Column(name = "tenant_realm", length = 64)
    private String tenantRealm;

    @Column(name = "keycloak_id", nullable = false, length = 64)
    private String keycloakId;

    @Column(name = "email", nullable = false, length = 64)
    private String email;

    @Column(name = "pending_sync", nullable = false)
    private boolean pendingSync;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    public User() {
        super();
    }

    public User(final String tenantRealm, final String keycloakId) {
        super();
        this.tenantRealm = tenantRealm;
        this.keycloakId = keycloakId;
    }

    @Override
    protected String uuidBase() {
        return "USR";
    }

}
