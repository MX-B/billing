package io.gr1d.billing.model.enumerations;

import io.gr1d.core.datasource.model.BaseEnum;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Raúl Sola
 */
@Entity
@NoArgsConstructor
@Table(name = "user_status")
public class UserStatus extends BaseEnum {

    public static final UserStatus ACTIVE = new UserStatus(1L, "ACTIVE");
    public static final UserStatus BLOCKED = new UserStatus(2L, "BLOCKED");

    private UserStatus(final Long id, final String name) {
        setId(id);
        setName(name);
    }

}
