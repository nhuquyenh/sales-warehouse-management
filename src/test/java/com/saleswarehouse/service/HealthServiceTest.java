package com.saleswarehouse.service;

import com.saleswarehouse.model.HealthStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HealthServiceTest {

    private final HealthService healthService = new HealthService();

    @Test
    void shouldReportApplicationAsUp() {
        HealthStatus status = healthService.getStatus();

        assertEquals("UP", status.status());
        assertEquals("Sales Warehouse Management", status.applicationName());
        assertFalse(status.javaVersion().isBlank());
    }
}
