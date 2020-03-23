package io.gr1d.billing.request;

import io.gr1d.billing.model.Address;
import io.gr1d.core.validation.Value;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@ToString
@Getter@Setter
public class AddressRequest {

    @NotEmpty
    @Length(max = 256)
    private String street;

    @Length(max = 128)
    private String complementary;

    @NotEmpty
    @Length(max = 16)
    private String streetNumber;

    @NotEmpty
    @Length(max = 128)
    private String neighborhood;

    @NotEmpty
    @Length(max = 128)
    private String city;

    @NotEmpty
    @Length(max = 128)
    private String state;

    @NotEmpty
    @Length(max = 32)
    private String zipcode;

    @NotEmpty
    @Length(max = 2)
    @Value(values = "br")
    private String country;

    public Address toAddress() {
        final Address address = new Address();
        address.setStreet(street);
        address.setComplementary(complementary);
        address.setStreetNumber(streetNumber);
        address.setNeighborhood(neighborhood);
        address.setCity(city);
        address.setState(state);
        address.setZipcode(zipcode);
        address.setCountry(country);
        return address;
    }

}
