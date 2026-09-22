package com.gregomebije.wallet.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;


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

    private String platformName;
    private String serviceName;
    private int cost;  //store in kobo
    private int revenue;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ServiceProvider(String platformName, String serviceName, int cost, int revenue) {
        this.platformName = platformName;
        this.serviceName = serviceName;
        this.cost = cost;
        this.revenue = revenue;
    }

}
