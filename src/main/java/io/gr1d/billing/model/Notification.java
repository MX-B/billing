package io.gr1d.billing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author Raúl Sola
 */
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "notification")
public class Notification {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "tries")
    private Long tries = 0L;

    @Lob
    @Column(name = "email")
    private byte[] email;

    public Notification(String templateName, byte[] email) {
        this.templateName = templateName;
        this.email = email;
    }
}
