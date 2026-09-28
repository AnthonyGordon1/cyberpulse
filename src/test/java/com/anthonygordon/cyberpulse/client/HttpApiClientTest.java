package com.anthonygordon.cyberpulse.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class HttpApiClientTest {

    private static final String URL = "https://api.example.com/data";

    private MockRestServiceServer mockServer;
    private HttpApiClient httpApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        httpApiClient = new HttpApiClient(builder.build());
    }

    /* ---------- Happy path ---------- */

    @Test
    void fetchData_returnsBody_onSuccess() {
        String expectedBody = "{\"vulnerabilities\":[]}";
        mockServer.expect(requestTo(URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(expectedBody, MediaType.APPLICATION_JSON));

        String result = httpApiClient.fetchData(URL);

        assertEquals(expectedBody, result);
        mockServer.verify();
    }

    /* ---------- Edge cases ---------- */

    @Test
    void fetchData_returnsNull_whenResponseHasNoBody() {
        mockServer.expect(requestTo(URL)).andRespond(withSuccess());

        String result = httpApiClient.fetchData(URL);

        assertNull(result);
    }

    /* ---------- Bad status codes ---------- */

    @Test
    void fetchData_wrapsAndRethrows_onClientError() {
        mockServer.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("invalid pubStartDate")
                        .contentType(MediaType.APPLICATION_JSON));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> httpApiClient.fetchData(URL));

        assertTrue(thrown.getMessage().contains(URL));
        assertTrue(thrown.getMessage().contains("400"));
        assertTrue(thrown.getMessage().contains("invalid pubStartDate"));
        assertInstanceOf(RestClientResponseException.class, thrown.getCause());
    }

    @Test
    void fetchData_wrapsAndRethrows_onRateLimit() {
        mockServer.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> httpApiClient.fetchData(URL));

        assertTrue(thrown.getMessage().contains("429"));
        assertInstanceOf(RestClientResponseException.class, thrown.getCause());
    }

    @Test
    void fetchData_wrapsAndRethrows_onServerError() {
        mockServer.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> httpApiClient.fetchData(URL));

        assertTrue(thrown.getMessage().contains(URL));
        assertTrue(thrown.getMessage().contains("503"));
        assertInstanceOf(RestClientResponseException.class, thrown.getCause());
    }

    /* ---------- Network failures ---------- */

    @Test
    void fetchData_wrapsAndRethrows_onTimeoutOrUnreachable() {
        mockServer.expect(requestTo(URL))
                .andRespond(request -> {
                    throw new IOException("Connect timed out");
                });

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> httpApiClient.fetchData(URL));

        assertTrue(thrown.getMessage().contains(URL));
        assertTrue(thrown.getMessage().contains("timed out or was unreachable"));
        assertInstanceOf(ResourceAccessException.class, thrown.getCause());
    }
}