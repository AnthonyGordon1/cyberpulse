package com.anthonygordon.cyberpulse.scheduler;

import com.anthonygordon.cyberpulse.service.CveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CveSyncSchedulerTest {

    @Mock
    private CveService cveService;

    @InjectMocks
    private CveSyncScheduler scheduler;

    /* ---------- Happy path ---------- */

    @Test
    void runScheduledSync_callsService() {
        when(cveService.syncRecentCves()).thenReturn(5);

        scheduler.runScheduledSync();

        verify(cveService, times(1)).syncRecentCves();
    }

    /* ---------- Failure handling ---------- */

    @Test
    void runScheduledSync_doesNotThrow_whenServiceFails() {
        when(cveService.syncRecentCves()).thenThrow(new RuntimeException("NVD unavailable"));

        assertDoesNotThrow(() -> scheduler.runScheduledSync());
    }

    @Test
    void runScheduledSync_canRunAgain_afterFailure() {
        when(cveService.syncRecentCves())
                .thenThrow(new RuntimeException("NVD unavailable"))
                .thenReturn(3);

        scheduler.runScheduledSync();
        scheduler.runScheduledSync();

        verify(cveService, times(2)).syncRecentCves();
    }
}