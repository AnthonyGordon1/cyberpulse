package com.anthonygordon.cyberpulse.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void fetchRecentCves_returnsNonBlankJson_fromLiveNvdApi() {
        String result = nvdClient.fetchRecentCves();
        assertTrue(result != null && result.contains("\"vulnerabilities\""));
    }
}
