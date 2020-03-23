package io.gr1d.billing.response;

import io.gr1d.billing.model.Address;
import lombok.Data;

import java.io.Serializable;

@Data
public class AddressResponse implements Serializable {

    private String street;
    private String complementary;
    private String streetNumber;
    private String neighborhood;
    private String city;
    private String state;
    private String zipcode;
    private String country;

    public AddressResponse(final Address address) {
        street = address.getStreet();
        complementary = address.getComplementary();
        streetNumber = address.getStreetNumber();
        neighborhood = address.getNeighborhood();
        city = address.getCity();
        state = address.getState();
        zipcode = address.getZipcode();
        country = address.getCountry();
    }

}
