package com.anthonygordon.cyberpulse.dto.nvd;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NvdResponse(Integer totalResults, List<Vulnerability> vulnerabilities) {
}
