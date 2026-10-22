package com.anthonygordon.cyberpulse.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class NvdClientTest {

    private static final Instant START = Instant.parse("2026-10-02T18:55:00Z");
    private static final Instant END = Instant.parse("2026-10-02T20:00:00Z");

    @Mock
    private ApiClient apiClient;
    private NvdClient nvdClient;

    @BeforeEach
    void setUp() {
        nvdClient = new NvdClient(apiClient);
    }

    /* ---------- Happy path ---------- */

    @Test
    void fetchModifiedCves_returnsBody_onSuccess() {
        String expectedBody = "{\"vulnerabilities\":[]}";
        when(apiClient.fetchData(anyString())).thenReturn(expectedBody);

        String result = nvdClient.fetchModifiedCves(START, END, 0);

        assertEquals(expectedBody, result);
    }

    /* ---------- Request building ---------- */

    @Test
    void fetchModifiedCves_sendsStartAndEndAsUtcLastModParams() {
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        nvdClient.fetchModifiedCves(START, END, 0);

        verify(apiClient).fetchData(urlCaptor.capture());
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUriString(urlCaptor.getValue()).build().getQueryParams();

        assertEquals("2026-10-02T18:55:00.000", params.getFirst("lastModStartDate"));
        assertEquals("2026-10-02T20:00:00.000", params.getFirst("lastModEndDate"));
        assertNull(params.getFirst("pubStartDate"));
    }

    /* ---------- Edge cases ---------- */

    @Test
    void fetchModifiedCves_returnsNull_whenApiClientReturnsNull() {
        when(apiClient.fetchData(anyString())).thenReturn(null);

        String result = nvdClient.fetchModifiedCves(START, END, 0);

        assertNull(result);
    }

    /* ---------- Failure handling ---------- */

    @Test
    void fetchModifiedCves_propagatesException_whenApiClientFails() {
        RuntimeException apiFailure = new RuntimeException("NVD down");
        when(apiClient.fetchData(anyString())).thenThrow(apiFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> nvdClient.fetchModifiedCves(START, END, 0));

        assertSame(apiFailure, thrown);
    }

    @Test
    void fetchModifiedCves_doesNotRetry_whenApiClientFails() {
        when(apiClient.fetchData(anyString())).thenThrow(new RuntimeException("NVD down"));

        assertThrows(RuntimeException.class, () -> nvdClient.fetchModifiedCves(START, END, 0));

        verify(apiClient, times(1)).fetchData(anyString());
    }
}
