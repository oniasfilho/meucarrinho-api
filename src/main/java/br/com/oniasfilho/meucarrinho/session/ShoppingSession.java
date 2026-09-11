package br.com.oniasfilho.meucarrinho.session;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "shopping_session")
public class ShoppingSession {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "store_name", length = 160)
    private String storeName;

    @Column(precision = 12, scale = 2)
    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShoppingSessionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ShoppingSession() {
    }

    public ShoppingSession(UUID id, AppUser user, String name, String storeName, BigDecimal budget) {
        this.id = id;
        this.user = user;
        this.name = name;
        this.storeName = storeName;
        this.budget = budget;
        this.status = ShoppingSessionStatus.ACTIVE;
    }

    public void updateSettings(String name, String storeName, BigDecimal budget) {
        this.name = name;
        this.storeName = storeName;
        this.budget = budget;
    }

    public void complete(Instant completedAt) {
        this.status = ShoppingSessionStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public String getStoreName() {
        return storeName;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public ShoppingSessionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}

