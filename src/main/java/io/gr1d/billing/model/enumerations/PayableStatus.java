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
@Table(name = "payable_status")
public class PayableStatus extends BaseEnum {

    public static final PayableStatus CREATED = new PayableStatus(1L, "CREATED");
    public static final PayableStatus IN_PROGRESS = new PayableStatus(2L, "IN_PROGRESS");
    public static final PayableStatus PAID = new PayableStatus(3L, "PAID");
    public static final PayableStatus PAID_AUTOMATICALLY = new PayableStatus(4L, "PAID_AUTOMATICALLY");

    private PayableStatus(final Long id, final String name) {
        setId(id);
        setName(name);
    }

}
