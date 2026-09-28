package com.anthonygordon.cyberpulse.client;

import java.time.Duration;
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

        public String fetchRecentCves() {
                Instant now = Instant.now();
                Instant twentyFourHoursAgo = now.minus(Duration.ofHours(24));

                String pubStartDate = NVD_DATE_FORMAT.format(twentyFourHoursAgo);
                String pubEndDate = NVD_DATE_FORMAT.format(now);

                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(NVD_BASE_URL);
                builder.queryParam("pubStartDate", pubStartDate)
                                .queryParam("pubEndDate", pubEndDate);
                String url = builder.toUriString();

                log.info("Fetching NVD CVEs published between {} and {}", pubStartDate, pubEndDate);
                String response = apiClient.fetchData(url);
                return response;
        }
}
