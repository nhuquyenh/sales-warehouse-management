package com.saleswarehouse.service;

import com.saleswarehouse.model.HealthStatus;
import com.saleswarehouse.util.ApplicationConstants;

public class HealthService {

    public HealthStatus getStatus() {
        return new HealthStatus(
                "UP",
                ApplicationConstants.APPLICATION_NAME,
                System.getProperty("java.version")
        );
    }
}
