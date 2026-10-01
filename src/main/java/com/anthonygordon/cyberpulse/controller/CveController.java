package com.anthonygordon.cyberpulse.controller;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;

import com.anthonygordon.cyberpulse.dto.SyncResult;
import com.anthonygordon.cyberpulse.model.Cve;
import com.anthonygordon.cyberpulse.service.CveService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CveController {
    private final CveService cveService;

    public CveController(CveService cveService) {
        this.cveService = cveService;
    }

    // Receive Request → CveController.getCves() — triggered automatically when GET
    // /api/cves is hit
    @GetMapping("/api/cves")
    public List<Cve> getCves() {
        // Ask Service Layer → CveController.getCves() calls CveService.getAllCves()
        return cveService.getAllCves();
    }

    @PostMapping("/api/cves/sync")
    public ResponseEntity<SyncResult> syncCves() {
        int syncedCount = cveService.syncRecentCves();
        return ResponseEntity.ok(new SyncResult(syncedCount));
    }
    
}
