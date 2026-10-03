package com.projexa.coaching.users.entity;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="permissions") public class Permission { @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Column(nullable=false,unique=true) private String code; public UUID getId(){return id;} public String getCode(){return code;} }
