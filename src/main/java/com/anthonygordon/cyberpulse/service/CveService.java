package com.anthonygordon.cyberpulse.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.anthonygordon.cyberpulse.model.Cve;
import com.anthonygordon.cyberpulse.repository.CveRepository;

@Service
public class CveService {
    private final CveRepository cveRepository;

    CveService(CveRepository cveRepository) {
        this.cveRepository = cveRepository;
    }

    // Service asks Repository → CveService.getAllCves() calls
    // CveRepository.findAll()
    public List<Cve> getAllCves() {
        // Return data → flows back up: Repository → Service → Controller → Spring
        // auto-converts to JSON → HTTP response
        return cveRepository.findAll();

    }
}