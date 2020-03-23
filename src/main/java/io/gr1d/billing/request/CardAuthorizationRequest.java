package io.gr1d.billing.request;

import io.gr1d.billing.validation.TenantExists;
import io.gr1d.core.validation.Value;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;

@Getter@Setter
@ToString(exclude = { "cardNumber", "cardExpirationDate", "cardCvv" })
public class CardAuthorizationRequest {

    @NotEmpty
    @TenantExists
    @Size(min = 1, max = 128)
    private String tenantRealm;

    private String userId;

    @NotEmpty
    @Pattern(regexp = "\\d{15,16}")
    private String cardNumber;

    @NotEmpty
    @Pattern(regexp = "\\d{4}")
    private String cardExpirationDate;

    @NotEmpty
    @Pattern(regexp = "\\d{3,4}")
    private String cardCvv;

    @NotEmpty
    @Size(max = 32)
    private String cardHolderName;

    @NotEmpty
    @Size(max = 64)
    private String fullName;

    @NotEmpty
    @Email
    @Size(max = 64)
    private String email;

    @NotEmpty
    @Value(values = {"CPF", "CNPJ"})
    private String documentType;

    @NotEmpty
    @Size(max = 24)
    private String document;

    @NotEmpty
    @Size(max = 24)
    @Pattern(regexp = "\\+\\d{3,23}")
    private String phone;

    /**
     * [MVP] Everyone is from Brazil for now - Country ISO Code (2 lowercase
     * letters)
     */
    @Pattern(regexp = "[a-z]{2}")
    private String nationality;

    @Past
    @NotNull
    private LocalDate birthDate;

    @Valid
    private AddressRequest address;

}


