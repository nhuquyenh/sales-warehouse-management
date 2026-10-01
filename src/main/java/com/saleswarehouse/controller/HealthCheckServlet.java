package com.saleswarehouse.controller;

import com.saleswarehouse.model.HealthStatus;
import com.saleswarehouse.service.HealthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "healthCheckServlet", urlPatterns = "/health")
public class HealthCheckServlet extends HttpServlet {

    private final HealthService healthService = new HealthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HealthStatus healthStatus = healthService.getStatus();

        request.setAttribute("healthStatus", healthStatus);
        request.setAttribute(
                "servletVersion",
                "%d.%d".formatted(
                        getServletContext().getMajorVersion(),
                        getServletContext().getMinorVersion()
                )
        );
        request.getRequestDispatcher("/views/health.jsp").forward(request, response);
    }
}
