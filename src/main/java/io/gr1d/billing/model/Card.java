package io.gr1d.billing.model;

import io.gr1d.billing.response.CardBrand;
import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

import static javax.persistence.CascadeType.PERSIST;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "card")
@EntityListeners(AuditListener.class)
public class Card extends BaseModel {

    @Column(name = "tenant_realm")
    private String tenantRealm;

    @ManyToOne(cascade = PERSIST)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "card_id", nullable = false, length = 64)
    private String cardId;

    @Column(name = "last_digits", nullable = false, length = 4)
    private String lastDigits;

    @Column(name = "card_holder_name", nullable = false, length = 64)
    private String cardHolderName;

    @Column(name = "full_name", nullable = false, length = 64)
    private String fullName;

    @Column(name = "document_type")
    private DocumentType documentType;

    @Column(nullable = false, length = 16)
    private String document;

    @Column(nullable = false, length = 24)
    private String phone;

    /**
     * [MVP] Everyone is from Brazil for now - Country ISO Code (2 lowercase
     * letters)
     */
    @Column(nullable = false, length = 4)
    private String nationality = "br";

    @Column(name = "birth_dt", nullable = false)
    private LocalDate birthDate;

    private CardBrand brand;

    @ManyToOne(cascade = PERSIST)
    @JoinColumn(name = "address_id")
    private Address address;

    @Override
    protected String uuidBase() {
        return "CRD";
    }

    public Card (final Card card) {
        this.tenantRealm = card.getTenantRealm();
        this.user = card.getUser();
        this.cardId = card.getCardId();
        this.lastDigits = card.getLastDigits();
        this.cardHolderName = card.getCardHolderName();
        this.fullName = card.getFullName();
        this.documentType = card.getDocumentType();
        this.document = card.getDocument();
        this.phone = card.getPhone();
        this.nationality  = card.getNationality();
        this.birthDate = card.getBirthDate();
        this.brand = card.getBrand();
        this.address = card.getAddress();
    }

}
