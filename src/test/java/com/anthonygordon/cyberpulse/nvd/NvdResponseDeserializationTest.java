package com.anthonygordon.cyberpulse.nvd;

import com.anthonygordon.cyberpulse.dto.nvd.CveItem;
import com.anthonygordon.cyberpulse.dto.nvd.CvssMetricV31;
import com.anthonygordon.cyberpulse.dto.nvd.NvdResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class NvdResponseDeserializationTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private NvdResponse loadSample() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/nvd/sample-response.json")) {
            return mapper.readValue(in, NvdResponse.class);
        }
    }

    /* ---------- Happy path ---------- */

    @Test
    void deserialize_mapsCveIdAndPublishedDate() throws Exception {
        CveItem cve = loadSample().vulnerabilities().get(0).cve();

        assertEquals("CVE-2026-0001", cve.id());
        assertEquals("2026-09-27T14:15:07.123", cve.published());
    }

    @Test
    void deserialize_mapsAllDescriptions() throws Exception {
        CveItem cve = loadSample().vulnerabilities().get(0).cve();

        assertEquals(2, cve.descriptions().size());
        assertEquals("en", cve.descriptions().get(0).lang());
        assertEquals("es", cve.descriptions().get(1).lang());
    }

    @Test
    void deserialize_mapsCvssV31Metrics() throws Exception {
        CveItem cve = loadSample().vulnerabilities().get(0).cve();
        CvssMetricV31 primary = cve.metrics().cvssMetricV31().get(0);

        assertEquals(2, cve.metrics().cvssMetricV31().size());
        assertEquals("Primary", primary.type());
        assertEquals(9.8, primary.cvssData().baseScore());
        assertEquals("CRITICAL", primary.cvssData().baseSeverity());
    }

    /* ---------- Edge cases ---------- */

    @Test
    void deserialize_leavesMetricsNull_whenCveHasNoScore() {
        String json = """
                { "vulnerabilities": [ { "cve": { "id": "CVE-2026-0002", "metrics": {} } } ] }
                """;

        CveItem cve = mapper.readValue(json, NvdResponse.class).vulnerabilities().get(0).cve();

        assertNull(cve.metrics().cvssMetricV31());
        assertNull(cve.descriptions());
    }
}