package com.anthonygordon.cyberpulse.controller;

import com.anthonygordon.cyberpulse.dto.SyncResult;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.anthonygordon.cyberpulse.model.Cve;
import com.anthonygordon.cyberpulse.service.CveService;

@ExtendWith(MockitoExtension.class)
class CveControllerTest {

    @Mock
    private CveService cveService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CveController(cveService)).build();
    }

    /* ---------- Sync endpoint ---------- */

    @Test
    void syncCves_returns200WithSavedCount() throws Exception {
        when(cveService.syncRecentCves()).thenReturn(new SyncResult(611, 600, 11));

        mockMvc.perform(post("/api/cves/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(611));

        verify(cveService, times(1)).syncRecentCves();
    }

    @Test
    void syncCves_rejectsGet() throws Exception {
        mockMvc.perform(get("/api/cves/sync"))
                .andExpect(status().isMethodNotAllowed());
    }

    /* ---------- List endpoint ---------- */

    @Test
    void getAllCves_returns200WithCves() throws Exception {
        Cve cve = new Cve();
        cve.setCveId("CVE-2026-0001");
        cve.setSeverity("HIGH");
        when(cveService.getAllCves()).thenReturn(List.of(cve));

        mockMvc.perform(get("/api/cves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cveId").value("CVE-2026-0001"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"));
    }
}