package com.anthonygordon.cyberpulse.client;

import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpApiClient implements ApiClient {
    private static final Logger log = LoggerFactory.getLogger(HttpApiClient.class);

    private final RestClient restClient;

    public HttpApiClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // Set connect timeout to 5 seconds
        factory.setReadTimeout(25000); // Set read timeout to 25 seconds for large datasets
        this.restClient = builder.requestFactory(factory).build();

    }

    @Override
    public String fetchData(String url) {

        log.info("Fetching data from URL: {}", url);

        try {
        
        ResponseEntity<String> response = restClient.get()
        .uri(url)
        .retrieve()
        .toEntity(String.class);

        return response.getBody();

        } 
        
        catch (Exception e) {
            log.error("Failed to fetch data from URL: {}", url, e);
            throw new RuntimeException("Failed to fetch data from URL: " + url, e);
        }
}