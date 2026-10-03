package com.anthonygordon.cyberpulse.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("integration")
class NvdClientIntegrationTest {

    @Autowired
    private NvdClient nvdClient;

    @Test
    void fetchModifiedCves_returnsNonBlankJson_fromLiveNvdApi() {
        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofHours(24));

        String result = nvdClient.fetchModifiedCves(start, end);
        assertTrue(result != null && result.contains("\"vulnerabilities\""));
    }
}
