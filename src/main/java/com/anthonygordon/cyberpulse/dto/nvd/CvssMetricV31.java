package com.anthonygordon.cyberpulse.dto.nvd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CvssMetricV31(String type, CvssDataV31 cvssData) {}
