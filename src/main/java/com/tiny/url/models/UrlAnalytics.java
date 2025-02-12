package com.tiny.url.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Data
public class UrlAnalytics {
    @Id
    private String urlId;
    
    private int totalClicks;
    
    @ElementCollection
    @CollectionTable(name = "url_referrers", 
        joinColumns = @JoinColumn(name = "url_id"))
    @MapKeyColumn(name = "referrer")
    @Column(name = "count")
    private Map<String, Integer> referrers = new HashMap<>();
    
    @ElementCollection
    @CollectionTable(name = "url_browsers", 
        joinColumns = @JoinColumn(name = "url_id"))
    @MapKeyColumn(name = "browser")
    @Column(name = "count")
    private Map<String, Integer> browsers = new HashMap<>();
    
    @ElementCollection
    @CollectionTable(name = "url_countries", 
        joinColumns = @JoinColumn(name = "url_id"))
    @MapKeyColumn(name = "country")
    @Column(name = "count")
    private Map<String, Integer> countries = new HashMap<>();
    
    @CreationTimestamp
    private LocalDateTime firstAccessed;
    
    @UpdateTimestamp
    private LocalDateTime lastAccessed;

    // Helper methods for incrementing counts
    public void incrementReferrer(String referrer) {
        referrers.merge(referrer, 1, Integer::sum);
        totalClicks++;
    }

    public void incrementBrowser(String browser) {
        browsers.merge(browser, 1, Integer::sum);
    }

    public void incrementCountry(String country) {
        countries.merge(country, 1, Integer::sum);
    }
} 