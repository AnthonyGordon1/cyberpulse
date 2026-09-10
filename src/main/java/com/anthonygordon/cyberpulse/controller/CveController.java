package com.anthonygordon.cyberpulse.controller;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import com.anthonygordon.cyberpulse.model.Cve;
import com.anthonygordon.cyberpulse.service.CveService;
import org.springframework.web.bind.annotation.GetMapping;

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
}
