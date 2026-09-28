package com.anthonygordon.cyberpulse.dto.nvd;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CveItem(
        String id,
        String published,
        List<Description> descriptions,
        Metrics metrics) {
}
