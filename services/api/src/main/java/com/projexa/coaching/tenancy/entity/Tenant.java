package com.projexa.coaching.tenancy.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="tenants")
public class Tenant {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false) private String name;
    @Column(nullable=false, unique=true) private String slug;
    @Column(nullable=false) private String status = "ACTIVE";
    private String timezone = "Asia/Kolkata";
    public UUID getId(){return id;} public String getName(){return name;} public String getSlug(){return slug;} public String getStatus(){return status;} public String getTimezone(){return timezone;}
}
