package com.saleswarehouse.controller;

import com.saleswarehouse.model.AuthenticatedUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(name = "dashboardServlet", urlPatterns = "/dashboard")
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        HttpSession session = request.getSession(false);
        if (session == null
                || !(session.getAttribute(LoginServlet.AUTHENTICATED_USER_SESSION_KEY)
                instanceof AuthenticatedUser authenticatedUser)) {
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/login"));
            return;
        }

        String roleDisplay = authenticatedUser.roles().isEmpty()
                ? "Chưa gán vai trò"
                : String.join(", ", authenticatedUser.roles());

        request.setAttribute("authenticatedUser", authenticatedUser);
        request.setAttribute("roleDisplay", roleDisplay);
        request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);
    }
}
