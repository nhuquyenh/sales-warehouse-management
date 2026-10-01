package com.saleswarehouse.controller;

import com.saleswarehouse.model.AuthenticatedUser;
import com.saleswarehouse.service.AuthService;
import com.saleswarehouse.service.AuthenticationException;
import com.saleswarehouse.service.AuthenticationFailure;
import com.saleswarehouse.service.AuthenticationResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Arrays;

@WebServlet(name = "loginServlet", urlPatterns = "/login", loadOnStartup = 1)
public class LoginServlet extends HttpServlet {

    public static final String AUTHENTICATED_USER_SESSION_KEY = "authenticatedUser";
    public static final String INVALID_CREDENTIALS_MESSAGE =
            "Tên đăng nhập hoặc mật khẩu không đúng";

    private static final String TEMPORARY_LOCK_MESSAGE =
            "Tài khoản đang bị khóa tạm thời. Vui lòng thử lại sau 15 phút.";
    private static final String ACCOUNT_UNAVAILABLE_MESSAGE =
            "Tài khoản không thể đăng nhập. Vui lòng liên hệ quản trị viên.";
    private static final String SYSTEM_ERROR_MESSAGE =
            "Không thể đăng nhập lúc này. Vui lòng thử lại sau.";

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        preventCaching(response);

        HttpSession session = request.getSession(false);
        if (session != null
                && session.getAttribute(AUTHENTICATED_USER_SESSION_KEY) instanceof AuthenticatedUser) {
            response.sendRedirect(response.encodeRedirectURL(
                    request.getContextPath() + "/dashboard"
            ));
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        preventCaching(response);

        String username = normalizeUsername(request.getParameter("username"));
        String rawPassword = request.getParameter("password");
        char[] password = rawPassword == null ? new char[0] : rawPassword.toCharArray();

        try {
            AuthenticationResult result = authService.authenticate(username, password);
            if (result.successful()) {
                createAuthenticatedSession(request, result.user());
                response.sendRedirect(response.encodeRedirectURL(
                        request.getContextPath() + "/dashboard"
                ));
                return;
            }

            request.setAttribute("loginUsername", username);
            request.setAttribute("errorMessage", messageFor(result.failure()));
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        } catch (AuthenticationException exception) {
            getServletContext().log("Login failed because authentication service is unavailable", exception);
            request.setAttribute("loginUsername", username);
            request.setAttribute("errorMessage", SYSTEM_ERROR_MESSAGE);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void createAuthenticatedSession(HttpServletRequest request, AuthenticatedUser user) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(AUTHENTICATED_USER_SESSION_KEY, user);
        session.setMaxInactiveInterval(30 * 60);
    }

    private String messageFor(AuthenticationFailure failure) {
        return switch (failure) {
            case INVALID_CREDENTIALS -> INVALID_CREDENTIALS_MESSAGE;
            case TEMPORARILY_LOCKED -> TEMPORARY_LOCK_MESSAGE;
            case ACCOUNT_LOCKED, ACCOUNT_INACTIVE -> ACCOUNT_UNAVAILABLE_MESSAGE;
        };
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private void preventCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }
}
