package com.anthonygordon.cyberpulse.client;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class NvdClient {

        private static final Logger log = LoggerFactory.getLogger(NvdClient.class);

        private static final DateTimeFormatter NVD_DATE_FORMAT = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

        private static final String NVD_BASE_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";
        private final ApiClient apiClient;

        public NvdClient(ApiClient apiClient) {
                this.apiClient = apiClient;
        }

        // Asks NVD for CVEs changed between start and end (new CVEs count as changed).
        // The caller picks the window, so this class never touches the database.
        public String fetchModifiedCves(Instant start, Instant end) {
                // NVD wants UTC timestamps like 2026-10-02T19:00:00.000
                String lastModStartDate = NVD_DATE_FORMAT.format(start);
                String lastModEndDate = NVD_DATE_FORMAT.format(end);

                // NVD needs both dates together, and the range can't pass 120 days
                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(NVD_BASE_URL);
                builder.queryParam("lastModStartDate", lastModStartDate)
                                .queryParam("lastModEndDate", lastModEndDate);
                String url = builder.toUriString();

                log.info("Fetching NVD CVEs modified between {} and {}", lastModStartDate, lastModEndDate);
                return apiClient.fetchData(url);
        }
}
