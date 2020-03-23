package io.gr1d.billing.model;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;

@Entity
@Getter@Setter
@Table(name = "address")
@EqualsAndHashCode
@EntityListeners(AuditListener.class)
public class Address extends BaseModel {

    private String street;
    private String complementary;
    @Column(name = "street_number")
    private String streetNumber;
    private String neighborhood;
    private String city;
    private String state;
    private String zipcode;
    private String country;

    public boolean hasSameConfig(final Address address) {
        return StringUtils.equals(address.getStreet(), getStreet())
                && StringUtils.equals(address.getCity(), getCity())
                && StringUtils.equals(address.getComplementary(), getComplementary())
                && StringUtils.equals(address.getCountry(), getCountry())
                && StringUtils.equals(address.getNeighborhood(), getNeighborhood())
                && StringUtils.equals(address.getState(), getState())
                && StringUtils.equals(address.getStreetNumber(), getStreetNumber())
                && StringUtils.equals(address.getZipcode(), getZipcode());
    }

    @Override
    protected String uuidBase() {
        return "ADDR";
    }

}
