package com.anthonygordon.cyberpulse.dto.nvd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CvssDataV31(Double baseScore, String baseSeverity) {
    
}
