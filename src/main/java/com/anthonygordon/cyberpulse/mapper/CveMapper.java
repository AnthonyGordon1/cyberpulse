package com.anthonygordon.cyberpulse.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Component;

import com.anthonygordon.cyberpulse.dto.nvd.CveItem;
import com.anthonygordon.cyberpulse.dto.nvd.CvssMetricV31;
import com.anthonygordon.cyberpulse.dto.nvd.Description;
import com.anthonygordon.cyberpulse.dto.nvd.Metrics;
import com.anthonygordon.cyberpulse.model.Cve;

@Component
public class CveMapper {

    public Cve toEntity(CveItem cveItem) {
        if (cveItem == null) {
            throw new IllegalArgumentException("CveItem must not be null");
        }
        if (cveItem.id() == null || cveItem.id().isBlank()) {
            throw new IllegalArgumentException("CveItem is missing an id");
        }

        Cve cve = new Cve();
        cve.setCveId(cveItem.id());
        cve.setSource("NVD");
        cve.setFetchedAt(LocalDateTime.now());

        if (cveItem.published() != null) {
            try {
                cve.setPublishedDate(LocalDateTime.parse(cveItem.published()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "CVE " + cveItem.id() + " has an invalid published date: " + cveItem.published(), e);
            }
        }

        cve.setDescription(pickEnglishDescription(cveItem.descriptions()));

        CvssMetricV31 metric = pickV31Metric(cveItem.metrics());
        if (metric != null && metric.cvssData() != null) {
            cve.setSeverity(metric.cvssData().baseSeverity());
            cve.setCvssScore(metric.cvssData().baseScore());
        }

        return cve;
    }

    private String pickEnglishDescription(List<Description> descriptions) {
        if (descriptions == null) {
            return null;
        }

        for (Description description : descriptions) {
            if ("en".equals(description.lang())) {
                return description.value();
            }
        }

        return null;
    }

    private CvssMetricV31 pickV31Metric(Metrics metrics) {
        if (metrics == null || metrics.cvssMetricV31() == null || metrics.cvssMetricV31().isEmpty()) {
            return null;
        }

        for (CvssMetricV31 metric : metrics.cvssMetricV31()) {
            if ("Primary".equals(metric.type())) {
                return metric;
            }
        }

        return metrics.cvssMetricV31().get(0);
    }
}