package com.anthonygordon.cyberpulse.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

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

    private static final Pattern NVD_DATE_PATTERN = Pattern
            .compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
    private static final DateTimeFormatter NVD_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    @Mock
    private ApiClient apiClient;
    private NvdClient nvdClient;

    @BeforeEach
    void setUp() {
        nvdClient = new NvdClient(apiClient);
    }

    /* ---------- Happy path ---------- */

    @Test
    void fetchRecentCves_returnsBody_onSuccess() {
        String expectedBody = "{\"vulnerabilities\":[]}";
        when(apiClient.fetchData(anyString())).thenReturn(expectedBody);

        String result = nvdClient.fetchRecentCves();

        assertEquals(expectedBody, result);
    }

    /* ---------- Request building ---------- */

    @Test
    void fetchRecentCves_sendsCorrectlyFormattedDateQueryParams() {
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        nvdClient.fetchRecentCves();

        verify(apiClient).fetchData(urlCaptor.capture());
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUriString(urlCaptor.getValue()).build().getQueryParams();

        String pubStartDate = params.getFirst("pubStartDate");
        String pubEndDate = params.getFirst("pubEndDate");

        assertTrue(pubStartDate != null && NVD_DATE_PATTERN.matcher(pubStartDate).matches());
        assertTrue(pubEndDate != null && NVD_DATE_PATTERN.matcher(pubEndDate).matches());

        LocalDateTime start = LocalDateTime.parse(pubStartDate, NVD_DATE_FORMAT);
        LocalDateTime end = LocalDateTime.parse(pubEndDate, NVD_DATE_FORMAT);
        long minutesBetween = Duration.between(start, end).toMinutes();

        assertTrue(Math.abs(minutesBetween - 24 * 60) < 2);
    }

    /* ---------- Edge cases ---------- */

    @Test
    void fetchRecentCves_returnsNull_whenApiClientReturnsNull() {
        when(apiClient.fetchData(anyString())).thenReturn(null);

        String result = nvdClient.fetchRecentCves();

        assertNull(result);
    }

    /* ---------- Failure handling ---------- */    

    @Test
    void fetchRecentCves_propagatesException_whenApiClientFails() {
        RuntimeException apiFailure = new RuntimeException("NVD down");
        when(apiClient.fetchData(anyString())).thenThrow(apiFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> nvdClient.fetchRecentCves());

        assertSame(apiFailure, thrown);
    }

    @Test
    void fetchRecentCves_doesNotRetry_whenApiClientFails() {
        when(apiClient.fetchData(anyString())).thenThrow(new RuntimeException("NVD down"));

        assertThrows(RuntimeException.class, () -> nvdClient.fetchRecentCves());

        verify(apiClient, times(1)).fetchData(anyString());
    }
}