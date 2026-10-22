package com.anthonygordon.cyberpulse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenReturn(TWO_CVES);

        int saved = cveService.syncRecentCves().saved();

        assertEquals(2, saved);
        verify(cveRepository).saveAll(savedCaptor.capture());
        assertEquals("CVE-2026-0001", savedCaptor.getValue().get(0).getCveId());
        assertEquals("CVE-2026-0002", savedCaptor.getValue().get(1).getCveId());
    }

    /* ---------- Sync window ---------- */

    @Test
    void syncRecentCves_asksForLastSevenDays_whenThereIsNoWatermark() {
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenReturn("{ \"vulnerabilities\": [] }");
        cveService.syncRecentCves();

        verify(nvdClient).fetchModifiedCves(startCaptor.capture(), endCaptor.capture(), eq(0));
        assertEquals(Duration.ofDays(7), Duration.between(startCaptor.getValue(), endCaptor.getValue()));
    }

    @Test
    void syncRecentCves_startsFiveMinutesBeforeWatermark_whenWatermarkExists() {
        SyncState watermark = new SyncState("NVD", LocalDateTime.of(2026, 10, 2, 19, 0));
        when(syncStateRepository.findById("NVD")).thenReturn(Optional.of(watermark));

        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenReturn("{ \"vulnerabilities\": [] }");
        cveService.syncRecentCves();

        verify(nvdClient).fetchModifiedCves(startCaptor.capture(), endCaptor.capture(), eq(0));
        assertEquals(Instant.parse("2026-10-02T18:55:00Z"), startCaptor.getValue());
    }

    /* ---------- Edge cases ---------- */

    @Test
    void syncRecentCves_skipsUnmappableCve_andSavesTheRest() {
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenReturn(ONE_GOOD_ONE_BAD);

        int saved = cveService.syncRecentCves().saved();

        assertEquals(1, saved);
        verify(cveRepository).saveAll(savedCaptor.capture());
        assertEquals("CVE-2026-0001", savedCaptor.getValue().get(0).getCveId());
    }

    @Test
    void syncRecentCves_throws_whenResponseIsEmpty() {
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenReturn("");

        assertThrows(IllegalStateException.class, () -> cveService.syncRecentCves());
        verify(cveRepository, never()).saveAll(any());
    }

    @Test
    void syncRecentCves_returnsZero_whenNoVulnerabilities() {
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenReturn("{ \"vulnerabilities\": [] }");

        assertEquals(0, cveService.syncRecentCves().saved());
        verify(cveRepository, never()).saveAll(any());
    }

    /* ---------- Failure handling ---------- */

    @Test
    void syncRecentCves_propagatesException_andSavesNothing_whenNvdFails() {
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenThrow(new RuntimeException("NVD down"));

        assertThrows(RuntimeException.class, () -> cveService.syncRecentCves());
        verify(cveRepository, never()).saveAll(any());
    }

    /* ---------- Watermark ---------- */

    @Test
    void syncRecentCves_doesNotSaveWatermark_whenNvdFails() {
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenThrow(new RuntimeException("NVD down"));

        assertThrows(RuntimeException.class, () -> cveService.syncRecentCves());

        verify(syncStateRepository, never()).save(any());
    }

    @Test
    void syncRecentCves_doesNotSaveWatermark_whenResponseIsBlank() {
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenReturn("");

        assertThrows(IllegalStateException.class, () -> cveService.syncRecentCves());

        verify(syncStateRepository, never()).save(any());
    }

    @Test
    void syncRecentCves_doesNotSaveWatermark_whenSavingCvesFails() {
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenReturn(TWO_CVES);
        doThrow(new RuntimeException("DB down")).when(cveRepository).saveAll(any());

        assertThrows(RuntimeException.class, () -> cveService.syncRecentCves());

        verify(syncStateRepository, never()).save(any());
    }

    @Test
    void syncRecentCves_savesWatermarkAtWindowEnd_afterCvesAreSaved() {
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenReturn(TWO_CVES);

        cveService.syncRecentCves();

        verify(nvdClient).fetchModifiedCves(startCaptor.capture(), endCaptor.capture(), eq(0));
        ArgumentCaptor<SyncState> stateCaptor = ArgumentCaptor.forClass(SyncState.class);
        InOrder order = inOrder(cveRepository, syncStateRepository);
        order.verify(cveRepository).saveAll(any());
        order.verify(syncStateRepository).save(stateCaptor.capture());
        assertEquals(LocalDateTime.ofInstant(endCaptor.getValue(), ZoneOffset.UTC),
                stateCaptor.getValue().getLastSuccessfulSync());
    }

    /* ---------- Pagination ---------- */

    private static String page(int totalResults, String... ids) {
        StringBuilder vulnerabilities = new StringBuilder();
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                vulnerabilities.append(",");
            }
            vulnerabilities.append("{ \"cve\": { \"id\": \"" + ids[i] + "\", \"published\": \"2026-09-27T14:15:07.123\","
                    + " \"descriptions\": [ { \"lang\": \"en\", \"value\": \"desc\" } ] } }");
        }
        return "{ \"totalResults\": " + totalResults + ", \"vulnerabilities\": [" + vulnerabilities + "] }";
    }

    @Test
    void syncRecentCves_fetchesEveryPage_andSavesAllCves() {
        when(nvdClient.fetchModifiedCves(any(), any(), eq(0))).thenReturn(page(3, "CVE-2026-0001", "CVE-2026-0002"));
        when(nvdClient.fetchModifiedCves(any(), any(), eq(2))).thenReturn(page(3, "CVE-2026-0003"));

        int saved = cveService.syncRecentCves().saved();

        assertEquals(3, saved);
        verify(cveRepository).saveAll(savedCaptor.capture());
        assertEquals(3, savedCaptor.getValue().size());
        verify(syncStateRepository).save(any());
    }

    @Test
    void syncRecentCves_savesNothingAndKeepsWatermark_whenALaterPageFails() {
        when(nvdClient.fetchModifiedCves(any(), any(), eq(0))).thenReturn(page(3, "CVE-2026-0001", "CVE-2026-0002"));
        when(nvdClient.fetchModifiedCves(any(), any(), eq(2))).thenThrow(new RuntimeException("NVD down"));

        assertThrows(RuntimeException.class, () -> cveService.syncRecentCves());

        verify(cveRepository, never()).saveAll(any());
        verify(syncStateRepository, never()).save(any());
    }

    @Test
    void syncRecentCves_stopsOnEmptyPage_evenIfTotalResultsIsTooHigh() {
        when(nvdClient.fetchModifiedCves(any(), any(), eq(0))).thenReturn(page(5, "CVE-2026-0001", "CVE-2026-0002"));
        when(nvdClient.fetchModifiedCves(any(), any(), eq(2))).thenReturn(page(5));

        int saved = cveService.syncRecentCves().saved();

        assertEquals(2, saved);
        verify(syncStateRepository).save(any());
    }

    /* ---------- Window clamp ---------- */

    @Test
    void syncRecentCves_clampsWindowToNvdLimit_whenWatermarkIsTooOld() {
        SyncState oldWatermark = new SyncState("NVD", LocalDateTime.now(ZoneOffset.UTC).minusDays(200));
        when(syncStateRepository.findById("NVD")).thenReturn(Optional.of(oldWatermark));
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt())).thenReturn("{ \"vulnerabilities\": [] }");

        cveService.syncRecentCves();

        verify(nvdClient).fetchModifiedCves(startCaptor.capture(), endCaptor.capture(), eq(0));
        assertEquals(Duration.ofDays(119), Duration.between(startCaptor.getValue(), endCaptor.getValue()));
    }

    @Test
    void syncRecentCves_countsNewAndUpdated_whenSomeCvesAlreadyExist() throws Exception {
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt()))
                .thenReturn(page(2, "CVE-2026-0001", "CVE-2026-0002"));
        Cve existing = new Cve();
        existing.setCveId("CVE-2026-0001");
        when(cveRepository.findAllById(any())).thenReturn(List.of(existing));

        var result = cveService.syncRecentCves();

        assertEquals(2, result.saved());
        assertEquals(1, result.created());
        assertEquals(1, result.updated());
    }

    @Test
    void syncRecentCves_countsAllAsNew_whenNothingExistsYet() throws Exception {
        when(nvdClient.fetchModifiedCves(any(), any(), anyInt()))
                .thenReturn(page(2, "CVE-2026-0001", "CVE-2026-0002"));
        when(cveRepository.findAllById(any())).thenReturn(List.of());

        var result = cveService.syncRecentCves();

        assertEquals(2, result.saved());
        assertEquals(2, result.created());
        assertEquals(0, result.updated());
    }
}
