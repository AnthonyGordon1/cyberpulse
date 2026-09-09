package com.anthonygordon.cyberpulse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "cve")
@Getter
@Setter
public class Cve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cve_id", nullable = false, unique = true)
    private String cveId;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String severity;

    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    private String source;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}