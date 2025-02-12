package com.tiny.url.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Setter
@Getter
@Data
public class Url {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(name = "tiny_url")
    private String tinyUrl;

    @Column(name = "original_url")
    private String originalUrl;

    @GeneratedValue
    @Column(name = "created_at", columnDefinition = "TIMESTAMP")
    @CreationTimestamp
    private Timestamp created_at;

    @GeneratedValue
    @Column(name = "updated_at", columnDefinition = "timestamp")
    @UpdateTimestamp
    private Timestamp updated_at;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "max_clicks")
    private Integer maxClicks;

    @Column(name = "click_count")
    private Integer clickCount = 0;

    public boolean isExpired() {
        if (expiryDate != null && LocalDateTime.now().isAfter(expiryDate)) {
            return true;
        }
        if (maxClicks != null && clickCount >= maxClicks) {
            return true;
        }
        return false;
    }
}
