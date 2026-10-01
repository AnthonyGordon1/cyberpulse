package com.anthonygordon.cyberpulse.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.anthonygordon.cyberpulse.client.NvdClient;
import com.anthonygordon.cyberpulse.dto.nvd.NvdResponse;
import com.anthonygordon.cyberpulse.dto.nvd.Vulnerability;
import com.anthonygordon.cyberpulse.mapper.CveMapper;
import com.anthonygordon.cyberpulse.model.Cve;
import com.anthonygordon.cyberpulse.repository.CveRepository;

import tools.jackson.databind.json.JsonMapper;

@Service
public class CveService {
    private static final Logger log = LoggerFactory.getLogger(CveService.class);

    private final CveRepository cveRepository;
    private final CveMapper cveMapper;
    private final NvdClient nvdClient;
    private final JsonMapper jsonMapper;

    public CveService(CveRepository cveRepository, CveMapper cveMapper, NvdClient nvdClient, JsonMapper jsonMapper) {
        this.cveRepository = cveRepository;
        this.cveMapper = cveMapper;
        this.nvdClient = nvdClient;
        this.jsonMapper = jsonMapper;
    }

    public List<Cve> getAllCves() {
        return cveRepository.findAll();
    }

    public int syncRecentCves() {
        log.info("Starting sync of recent CVEs");

        // 1. Fetch the raw JSON from NVD
        String json = nvdClient.fetchRecentCves();

        // 2. If the JSON is null or blank, log a warning and return 0
        // (that's the null case your NvdClient and HttpApiClient tests flagged)

        if (json == null || json.isBlank()) {
            // Log a warning and return 0
            log.warn("NVD returned an empty response, nothing to sync");
            return 0;
        }

        // 3. Parse the JSON into an NvdResponse
        NvdResponse nvdResponse = jsonMapper.readValue(json, NvdResponse.class);

        // 4. If there are no vulnerabilities, return 0
        if (nvdResponse == null || nvdResponse.vulnerabilities() == null || nvdResponse.vulnerabilities().isEmpty()) {
            log.info("No vulnerabilities found in the NVD response");
            return 0;
        }

        // 5. Create an empty List<Cve>
        // Loop over each vulnerability:
        // map its cve() to a Cve entity and add it to the list
        List<Cve> cves = new ArrayList<>();
        for (Vulnerability vulnerability : nvdResponse.vulnerabilities()) {
            try {
                cves.add(cveMapper.toEntity(vulnerability.cve()));
            } catch (IllegalArgumentException e) {
                log.warn("Skipping CVE that could not be mapped: {}", e.getMessage());
            }
        }

        // 6. Save the whole list to the database
        cveRepository.saveAll(cves);

        // 7. Log how many were saved, and return that number
        log.info("Saved {} CVEs to the database", cves.size());

        return cves.size();
    }
}