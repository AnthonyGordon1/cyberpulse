-- CVE Table
CREATE TABLE IF NOT EXISTS cves (
    cve_id            VARCHAR(255) PRIMARY KEY,
    description       TEXT,
    severity          VARCHAR(255),
    cvss_score        DOUBLE PRECISION,
    affected_vendor   VARCHAR(255),
    affected_product  VARCHAR(255),
    published_date    TIMESTAMP,
    source            VARCHAR(255),
    fetched_at        TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sync_state (
    source VARCHAR(255) PRIMARY KEY,
    last_successful_sync TIMESTAMP NOT NULL
    
);