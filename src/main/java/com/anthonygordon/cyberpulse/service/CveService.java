package com.anthonygordon.cyberpulse.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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
import com.anthonygordon.cyberpulse.repository.SyncStateRepository;

import tools.jackson.databind.json.JsonMapper;

@Service
public class CveService {
    private static final Logger log = LoggerFactory.getLogger(CveService.class);

    private static final String NVD_SOURCE = "NVD";
    private static final Duration SYNC_OVERLAP = Duration.ofMinutes(5);
    private static final Duration FIRST_RUN_BACKFILL = Duration.ofDays(7);

    private final CveRepository cveRepository;
    private final CveMapper cveMapper;
    private final NvdClient nvdClient;
    private final JsonMapper jsonMapper;
    private final SyncStateRepository syncStateRepository;

    public CveService(CveRepository cveRepository, CveMapper cveMapper,
            NvdClient nvdClient, JsonMapper jsonMapper,
            SyncStateRepository syncStateRepository) {
        this.cveRepository = cveRepository;
        this.cveMapper = cveMapper;
        this.nvdClient = nvdClient;
        this.jsonMapper = jsonMapper;
        this.syncStateRepository = syncStateRepository;
    }

    public List<Cve> getAllCves() {
        return cveRepository.findAll();
    }

    public int syncRecentCves() {
        log.info("Starting sync of recent CVEs");

        // Work out the window to ask NVD about
        // End is captured before the call so the watermark can use it later
        Instant end = Instant.now();
        Instant start = findSyncStart(end);

        // 1. Fetch the raw JSON from NVD
        String json = nvdClient.fetchModifiedCves(start, end);

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

    // No watermark row means first run, so go back 7 days
    // Otherwise start at the watermark minus the overlap (stored as UTC)
    private Instant findSyncStart(Instant now) {
        return syncStateRepository.findById(NVD_SOURCE)
                .map(state -> state.getLastSuccessfulSync().toInstant(ZoneOffset.UTC).minus(SYNC_OVERLAP))
                .orElse(now.minus(FIRST_RUN_BACKFILL));
    }
}