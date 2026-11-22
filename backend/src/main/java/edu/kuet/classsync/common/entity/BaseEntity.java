package edu.kuet.classsync.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Common id and audit timestamps shared by every entity. */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }

    /** For tests and mappers only; JPA generates ids in production. */
    public void setId(Long id) { this.id = id; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
