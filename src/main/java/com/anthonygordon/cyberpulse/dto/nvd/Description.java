package com.anthonygordon.cyberpulse.dto.nvd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Description(String lang, String value) {}