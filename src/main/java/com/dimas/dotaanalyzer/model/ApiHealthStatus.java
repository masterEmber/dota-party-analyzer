package com.dimas.dotaanalyzer.model;

import java.time.Instant;

public record ApiHealthStatus(
        String status,
        String baseUrl,
        String testedEndpoint,
        Integer httpStatus,
        long latencyMs,
        Instant checkedAt,
        String message
) {
    public boolean isUp() {
        return "UP".equals(status);
    }
}
