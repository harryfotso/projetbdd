package com.cwa.projetbdd.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "classement",
       uniqueConstraints = @UniqueConstraint(columnNames = {"periode", "type_periode"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Classement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cid")
    private Integer cid;

    @NotBlank
    @Size(max = 20)
    @Column(name = "periode", nullable = false, length = 20)
    private String periode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_periode", nullable = false)
    private TypePeriode typePeriode;
}
