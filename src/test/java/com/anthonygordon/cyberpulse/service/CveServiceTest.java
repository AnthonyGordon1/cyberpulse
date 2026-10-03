package com.anthonygordon.cyberpulse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anthonygordon.cyberpulse.client.NvdClient;
import com.anthonygordon.cyberpulse.mapper.CveMapper;
import com.anthonygordon.cyberpulse.model.Cve;
import com.anthonygordon.cyberpulse.model.SyncState;
import com.anthonygordon.cyberpulse.repository.CveRepository;
import com.anthonygordon.cyberpulse.repository.SyncStateRepository;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CveServiceTest {

    @Mock
    private NvdClient nvdClient;
    @Mock
    private CveRepository cveRepository;
    @Mock
    private SyncStateRepository syncStateRepository;
    @Captor
    private ArgumentCaptor<List<Cve>> savedCaptor;
    @Captor
    private ArgumentCaptor<Instant> startCaptor;
    @Captor
    private ArgumentCaptor<Instant> endCaptor;

    private CveService cveService;

    @BeforeEach
    void setUp() {
        cveService = new CveService(cveRepository, new CveMapper(), nvdClient, JsonMapper.builder().build(),
                syncStateRepository);
    }

    private static final String TWO_CVES = """
            { "vulnerabilities": [
                { "cve": { "id": "CVE-2026-0001", "published": "2026-09-27T14:15:07.123",
                           "descriptions": [ { "lang": "en", "value": "First" } ] } },
                { "cve": { "id": "CVE-2026-0002", "published": "2026-09-27T15:00:00.000",
                           "descriptions": [ { "lang": "en", "value": "Second" } ] } }
            ] }
            """;

    private static final String ONE_GOOD_ONE_BAD = """
            { "vulnerabilities": [
                { "cve": { "id": "CVE-2026-0001", "published": "2026-09-27T14:15:07.123" } },
                { "cve": { "id": "CVE-2026-0002", "published": "not-a-date" } }
            ] }
            """;

    /* ---------- Happy path ---------- */

    @Test
    void syncRecentCves_savesAllCves_andReturnsCount() {
        when(nvdClient.fetchModifiedCves(any(), any())).thenReturn(TWO_CVES);

        int saved = cveService.syncRecentCves();

        assertEquals(2, saved);
        verify(cveRepository).saveAll(savedCaptor.capture());
        assertEquals("CVE-2026-0001", savedCaptor.getValue().get(0).getCveId());
        assertEquals("CVE-2026-0002", savedCaptor.getValue().get(1).getCveId());
    }

    /* ---------- Sync window ---------- */

    @Test
    void syncRecentCves_asksForLastSevenDays_whenThereIsNoWatermark() {
        cveService.syncRecentCves();

        verify(nvdClient).fetchModifiedCves(startCaptor.capture(), endCaptor.capture());
        assertEquals(Duration.ofDays(7), Duration.between(startCaptor.getValue(), endCaptor.getValue()));
    }

    @Test
    void syncRecentCves_startsFiveMinutesBeforeWatermark_whenWatermarkExists() {
        SyncState watermark = new SyncState("NVD", LocalDateTime.of(2026, 10, 2, 19, 0));
        when(syncStateRepository.findById("NVD")).thenReturn(Optional.of(watermark));

        cveService.syncRecentCves();

        verify(nvdClient).fetchModifiedCves(startCaptor.capture(), endCaptor.capture());
        assertEquals(Instant.parse("2026-10-02T18:55:00Z"), startCaptor.getValue());
    }

    /* ---------- Edge cases ---------- */

    @Test
    void syncRecentCves_skipsUnmappableCve_andSavesTheRest() {
        when(nvdClient.fetchModifiedCves(any(), any())).thenReturn(ONE_GOOD_ONE_BAD);

        int saved = cveService.syncRecentCves();

        assertEquals(1, saved);
        verify(cveRepository).saveAll(savedCaptor.capture());
        assertEquals("CVE-2026-0001", savedCaptor.getValue().get(0).getCveId());
    }

    @Test
    void syncRecentCves_returnsZero_whenResponseIsEmpty() {
        when(nvdClient.fetchModifiedCves(any(), any())).thenReturn("");

        assertEquals(0, cveService.syncRecentCves());
        verify(cveRepository, never()).saveAll(any());
    }

    @Test
    void syncRecentCves_returnsZero_whenNoVulnerabilities() {
        when(nvdClient.fetchModifiedCves(any(), any())).thenReturn("{ \"vulnerabilities\": [] }");

        assertEquals(0, cveService.syncRecentCves());
        verify(cveRepository, never()).saveAll(any());
    }

    /* ---------- Failure handling ---------- */

    @Test
    void syncRecentCves_propagatesException_andSavesNothing_whenNvdFails() {
        when(nvdClient.fetchModifiedCves(any(), any())).thenThrow(new RuntimeException("NVD down"));

        assertThrows(RuntimeException.class, () -> cveService.syncRecentCves());
        verify(cveRepository, never()).saveAll(any());
    }
}
