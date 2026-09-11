package br.com.oniasfilho.meucarrinho.session.item;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.session.ShoppingSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "shopping_session_item")
public class ShoppingSessionItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ShoppingSession session;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "label_photo_key", columnDefinition = "text")
    private String labelPhotoKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShoppingSessionItem() {
    }

    public ShoppingSessionItem(
        UUID id,
        ShoppingSession session,
        String name,
        BigDecimal unitPrice,
        int quantity,
        String note,
        String labelPhotoKey
    ) {
        this.id = id;
        this.session = session;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.note = note;
        this.labelPhotoKey = labelPhotoKey;
    }

    public void update(String name, BigDecimal unitPrice, int quantity, String note) {
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.note = note;
    }

    public void assignLabelPhotoKey(String labelPhotoKey) {
        this.labelPhotoKey = labelPhotoKey;
    }

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
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

    public ShoppingSession getSession() {
        return session;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getNote() {
        return note;
    }

    public String getLabelPhotoKey() {
        return labelPhotoKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

