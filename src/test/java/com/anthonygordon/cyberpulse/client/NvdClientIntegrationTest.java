package com.anthonygordon.cyberpulse.client;

import static org.junit.jupiter.api.Assertions.assertFalse;

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

        System.out.println("NVD response body: " + result);
        assertFalse(result == null || result.isBlank());
    }
}
