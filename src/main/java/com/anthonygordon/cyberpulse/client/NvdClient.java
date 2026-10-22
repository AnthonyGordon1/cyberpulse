package com.anthonygordon.cyberpulse.client;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NvdClient {

        private static final Logger log = LoggerFactory.getLogger(NvdClient.class);

        private static final DateTimeFormatter NVD_DATE_FORMAT = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

        private static final String NVD_BASE_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";
        private static final int PAGE_SIZE = 2000; // NVD maximum

        private final ApiClient apiClient;

        public NvdClient(ApiClient apiClient) {
                this.apiClient = apiClient;
        }

        // Asks NVD for one page of CVEs changed between start and end (new CVEs count
        // as changed).
        // The caller picks the window and loops over pages, so this class never touches
        // the database.
        public String fetchModifiedCves(Instant start, Instant end, int startIndex) {
                String url = UriComponentsBuilder.fromUriString(NVD_BASE_URL)
                                .queryParam("lastModStartDate", NVD_DATE_FORMAT.format(start))
                                .queryParam("lastModEndDate", NVD_DATE_FORMAT.format(end))
                                .queryParam("resultsPerPage", PAGE_SIZE)
                                .queryParam("startIndex", startIndex)
                                .build()
                                .toUriString();
                log.debug("Fetching NVD page at startIndex {}", startIndex);
                return apiClient.fetchData(url);
        }
}