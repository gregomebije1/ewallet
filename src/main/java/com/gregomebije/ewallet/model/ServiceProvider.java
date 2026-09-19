package com.gregomebije.ewallet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "service_providers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Required by JPA, protected prevents direct instantiation
@AllArgsConstructor
@Builder // Optional: Gives you a clean fluent API to create objects without setters

public class ServiceProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false, unique = true)
    private Long id;

    private String name;
    private String platformName;
    private int cost;  //store in kobo
    private int revenue;

    @Column(nullable = false, insertable = false, updatable = false, columnDefinition = "TIMESTAMP CURRENT DEFAULT TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = false, insertable = false, updatable = false, columnDefinition = "TIMESTAMP CURRENT DEFAULT TIMESTAMP")
    private LocalDateTime updatedAt;

}
