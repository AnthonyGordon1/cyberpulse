package com.anthonygordon.cyberpulse.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.anthonygordon.cyberpulse.dto.nvd.CveItem;
import com.anthonygordon.cyberpulse.dto.nvd.CvssDataV31;
import com.anthonygordon.cyberpulse.dto.nvd.CvssMetricV31;
import com.anthonygordon.cyberpulse.dto.nvd.Description;
import com.anthonygordon.cyberpulse.dto.nvd.Metrics;
import com.anthonygordon.cyberpulse.model.Cve;

class CveMapperTest {

    private final CveMapper mapper = new CveMapper();

    private static final String ID = "CVE-2026-0001";
    private static final String PUBLISHED = "2026-09-27T14:15:07.123";

    private static final List<Description> EN_AND_ES = List.of(
            new Description("es", "Descripción en español"),
            new Description("en", "English description"));

    private static CvssMetricV31 metric(String type, Double score, String severity) {
        return new CvssMetricV31(type, new CvssDataV31(score, severity));
    }

    /* ---------- Happy path ---------- */

    @Test
    void toEntity_mapsAllFields_whenEverythingPresent() {
        CveItem item = new CveItem(ID, PUBLISHED, EN_AND_ES,
                new Metrics(List.of(metric("Primary", 9.8, "CRITICAL"))));

        Cve cve = mapper.toEntity(item);

        assertEquals(ID, cve.getCveId());
        assertEquals("NVD", cve.getSource());
        assertNotNull(cve.getFetchedAt());
        assertEquals(LocalDateTime.of(2026, 9, 27, 14, 15, 7, 123_000_000), cve.getPublishedDate());
        assertEquals("English description", cve.getDescription());
        assertEquals("CRITICAL", cve.getSeverity());
        assertEquals(9.8, cve.getCvssScore());
    }

    /* ---------- Description selection ---------- */

    @Test
    void toEntity_leavesDescriptionNull_whenDescriptionsMissing() {
        CveItem item = new CveItem(ID, PUBLISHED, null, null);

        assertNull(mapper.toEntity(item).getDescription());
    }

    @Test
    void toEntity_leavesDescriptionNull_whenNoEnglishDescription() {
        CveItem item = new CveItem(ID, PUBLISHED,
                List.of(new Description("es", "Solo español")), null);

        assertNull(mapper.toEntity(item).getDescription());
    }

    /* ---------- Score selection ---------- */

    @Test
    void toEntity_picksPrimaryScore_evenWhenNotFirst() {
        CveItem item = new CveItem(ID, PUBLISHED, EN_AND_ES, new Metrics(List.of(
                metric("Secondary", 8.1, "HIGH"),
                metric("Primary", 9.8, "CRITICAL"))));

        Cve cve = mapper.toEntity(item);

        assertEquals("CRITICAL", cve.getSeverity());
        assertEquals(9.8, cve.getCvssScore());
    }

    @Test
    void toEntity_fallsBackToFirstScore_whenNoPrimary() {
        CveItem item = new CveItem(ID, PUBLISHED, EN_AND_ES,
                new Metrics(List.of(metric("Secondary", 8.1, "HIGH"))));

        Cve cve = mapper.toEntity(item);

        assertEquals("HIGH", cve.getSeverity());
        assertEquals(8.1, cve.getCvssScore());
    }

    @Test
    void toEntity_leavesScoreNull_whenNoV31Metrics() {
        CveItem item = new CveItem(ID, PUBLISHED, EN_AND_ES, new Metrics(null));

        Cve cve = mapper.toEntity(item);

        assertNull(cve.getSeverity());
        assertNull(cve.getCvssScore());
    }

    /* ---------- Edge cases ---------- */

    @Test
    void toEntity_leavesPublishedDateNull_whenPublishedMissing() {
        CveItem item = new CveItem(ID, null, EN_AND_ES, null);

        assertNull(mapper.toEntity(item).getPublishedDate());
    }

    /* ---------- Failure handling ---------- */

    @Test
    void toEntity_throws_whenItemIsNull() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toEntity(null));
    }

    @Test
    void toEntity_throws_whenIdIsBlank() {
        CveItem item = new CveItem("   ", PUBLISHED, EN_AND_ES, null);

        assertThrows(IllegalArgumentException.class, () -> mapper.toEntity(item));
    }

    @Test
    void toEntity_throwsWithCveId_whenPublishedDateIsMalformed() {
        CveItem item = new CveItem(ID, "not-a-date", EN_AND_ES, null);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> mapper.toEntity(item));

        assertTrue(thrown.getMessage().contains(ID));
        assertTrue(thrown.getMessage().contains("not-a-date"));
    }
}