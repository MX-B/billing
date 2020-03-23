package io.gr1d.billing.model.transfer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gr1d.billing.api.subscriptions.Provider;
import io.gr1d.core.datasource.audit.AuditListener;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "transfer_letter_provider_data")
public class TransferLetterProviderData implements Serializable {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "transfer_letter_provider_id", nullable = false)
    private TransferLetterProvider transferLetterProvider;

    @Column(name = "name")
    private String name;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "bank")
    private String bank;

    @Column(name = "agency")
    private String agency;

    @Column(name = "account")
    private String account;

    public TransferLetterProviderData(final Provider provider, final TransferLetterProvider transferLetterProvider) {
        this.name = provider.getName();
        this.phone = provider.getPhone();
        this.email = provider.getEmail();

        this.documentType = provider.getWallet().getDocumentType();
        this.documentNumber = provider.getWallet().getDocumentNumber();
        this.bank = provider.getWallet().getBankName();
        this.agency = provider.getWallet().getAgency();
        this.account = provider.getWallet().getBankAccount();
        this.transferLetterProvider = transferLetterProvider;

    }
}
