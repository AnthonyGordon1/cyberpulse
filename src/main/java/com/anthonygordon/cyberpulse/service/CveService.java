package com.anthonygordon.cyberpulse.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
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
import com.anthonygordon.cyberpulse.model.SyncState;
import com.anthonygordon.cyberpulse.repository.CveRepository;
import com.anthonygordon.cyberpulse.repository.SyncStateRepository;

import tools.jackson.databind.json.JsonMapper;

@Service
public class CveService {
    private static final Logger log = LoggerFactory.getLogger(CveService.class);

    private static final String NVD_SOURCE = "NVD";
    private static final Duration SYNC_OVERLAP = Duration.ofMinutes(5);
    private static final Duration FIRST_RUN_BACKFILL = Duration.ofDays(7);
    private static final Duration MAX_WINDOW = Duration.ofDays(119); // NVD rejects ranges over 120 days

    private final CveRepository cveRepository;
    private final CveMapper cveMapper;
    private final NvdClient nvdClient;
    private final JsonMapper jsonMapper;
    private final SyncStateRepository syncStateRepository;

    public CveService(CveRepository cveRepository, CveMapper cveMapper, NvdClient nvdClient, JsonMapper jsonMapper,
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
        // End is captured before the first call so the watermark can use it later
        Instant end = Instant.now();
        Instant start = findSyncStart(end);

        // Collect every page of the window before saving anything
        List<Cve> cves = new ArrayList<>();
        int startIndex = 0;
        int totalResults = 0;

        do {
            // 1. Fetch one page of raw JSON from NVD
            String json = nvdClient.fetchModifiedCves(start, end, startIndex);

            // 2. A blank page means we did not get the whole window
            // Throw so the watermark stays put and the next run retries it
            if (json == null || json.isBlank()) {
                throw new IllegalStateException("NVD returned an empty response at startIndex " + startIndex);
            }

            // 3. Parse the JSON into an NvdResponse
            NvdResponse page = jsonMapper.readValue(json, NvdResponse.class);
            if (page == null) {
                break;
            }
            totalResults = page.totalResults() == null ? 0 : page.totalResults();

            // 4. No vulnerabilities on this page: stop instead of looping forever
            List<Vulnerability> vulnerabilities = page.vulnerabilities();
            if (vulnerabilities == null || vulnerabilities.isEmpty()) {
                break;
            }

            // 5. Map each vulnerability's cve() to a Cve entity
            for (Vulnerability vulnerability : vulnerabilities) {
                try {
                    cves.add(cveMapper.toEntity(vulnerability.cve()));
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping CVE that could not be mapped: {}", e.getMessage());
                }
            }

            // 6. Move to the next page
            startIndex += vulnerabilities.size();
        } while (startIndex < totalResults);

        // 7. Save everything we collected
        if (!cves.isEmpty()) {
            cveRepository.saveAll(cves);
        }

        // Every page was fetched and saved, so now it is safe to move the watermark
        // If anything above threw, we never get here and the next run repeats the
        // window
        saveWatermark(end);

        log.info("Saved {} CVEs to the database", cves.size());

        return cves.size();
    }

    // No watermark row means first run, so go back 7 days
    // Otherwise start at the watermark minus the overlap (stored as UTC)
    private Instant findSyncStart(Instant now) {
        Instant start = syncStateRepository.findById(NVD_SOURCE)
                .map(state -> state.getLastSuccessfulSync().toInstant(ZoneOffset.UTC).minus(SYNC_OVERLAP))
                .orElse(now.minus(FIRST_RUN_BACKFILL));

        // NVD rejects last-modified ranges longer than 120 days
        Instant earliest = now.minus(MAX_WINDOW);
        if (start.isBefore(earliest)) {
            log.warn("Watermark is older than the NVD limit, only syncing the last {} days", MAX_WINDOW.toDays());
            return earliest;
        }
        return start;
    }

    // Stores the end of this run's window as the new watermark (UTC, no zone kept)
    private void saveWatermark(Instant end) {
        LocalDateTime lastSync = LocalDateTime.ofInstant(end, ZoneOffset.UTC);
        syncStateRepository.save(new SyncState(NVD_SOURCE, lastSync));
    }
}
