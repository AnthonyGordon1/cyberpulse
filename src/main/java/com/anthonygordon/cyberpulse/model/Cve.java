package com.anthonygordon.cyberpulse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "cves")
@Getter
@Setter
public class Cve {

    @Id
    @Column(name = "cve_id", nullable = false)
    private String cveId;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String severity;

    @Column(name = "cvss_score")
    private Double cvssScore;

    @Column(name = "affected_vendor")
    private String affectedVendor;

    @Column(name = "affected_product")
    private String affectedProduct;

    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    private String source;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;
}