package org.mdental.authcore.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "realms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Realm extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "clinic_slug", nullable = false)
    private String clinicSlug;

    @Column(name = "issuer", nullable = false)
    private String issuer;

    @Column(name = "admin_username", nullable = false)
    private String adminUsername;
}