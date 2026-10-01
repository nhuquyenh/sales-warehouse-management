package com.saleswarehouse.service;

import com.saleswarehouse.util.DatabaseConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class AuthServiceIntegrationTest {

    private static final String ADMIN_USERNAME = "admin";

    private final DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory();
    private final AuthService authService = new AuthService();
    private String adminPassword;

    @BeforeEach
    void setUp() throws SQLException {
        adminPassword = requiredEnvironmentVariable("TEST_ADMIN_PASSWORD");
        resetAdminState();
    }

    @AfterEach
    void tearDown() throws SQLException {
        resetAdminState();
    }

    @Test
    void shouldLoginWithCorrectAdminCredentials() {
        AuthenticationResult result = authenticate(ADMIN_USERNAME, adminPassword);

        assertTrue(result.successful());
        assertNotNull(result.user());
        assertEquals(ADMIN_USERNAME, result.user().username());
        assertEquals("Sprint 1 Administrator", result.user().fullName());
        assertTrue(result.user().roles().contains("ADMIN"));
    }

    @Test
    void shouldRecordOneFailedAttemptForWrongPassword() throws SQLException {
        AuthenticationResult result = authenticate(ADMIN_USERNAME, "wrong-password");
        LoginSecurityState state = readSecurityState();

        assertFalse(result.successful());
        assertEquals(AuthenticationFailure.INVALID_CREDENTIALS, result.failure());
        assertEquals(1, state.failedAttempts());
        assertNull(state.temporaryLockedUntil());
    }

    @Test
    void shouldTemporarilyLockAccountAfterFiveConsecutiveFailures() throws SQLException {
        LocalDateTime startedAt = LocalDateTime.now();

        for (int attempt = 1; attempt <= 5; attempt++) {
            AuthenticationResult result = authenticate(ADMIN_USERNAME, "wrong-password");
            assertFalse(result.successful());
            assertEquals(AuthenticationFailure.INVALID_CREDENTIALS, result.failure());
        }

        LoginSecurityState state = readSecurityState();
        assertEquals(5, state.failedAttempts());
        assertNotNull(state.temporaryLockedUntil());
        assertTrue(state.temporaryLockedUntil().isAfter(startedAt.plusMinutes(14)));
        assertTrue(state.temporaryLockedUntil().isBefore(LocalDateTime.now().plusMinutes(16)));
    }

    @Test
    void shouldRejectCorrectPasswordDuringTemporaryLock() throws SQLException {
        setTemporaryLock(LocalDateTime.now().plusMinutes(15));

        AuthenticationResult result = authenticate(ADMIN_USERNAME, adminPassword);

        assertFalse(result.successful());
        assertEquals(AuthenticationFailure.TEMPORARILY_LOCKED, result.failure());
    }

    @Test
    void shouldRejectAdministrativelyLockedAccount() throws SQLException {
        setAccountStatusLocked();

        AuthenticationResult result = authenticate(ADMIN_USERNAME, adminPassword);

        assertFalse(result.successful());
        assertEquals(AuthenticationFailure.ACCOUNT_LOCKED, result.failure());
    }

    @Test
    void shouldResetFailedAttemptsAfterSuccessfulLogin() throws SQLException {
        setFailedAttempts(3);

        AuthenticationResult result = authenticate(ADMIN_USERNAME, adminPassword);
        LoginSecurityState state = readSecurityState();

        assertTrue(result.successful());
        assertEquals(0, state.failedAttempts());
        assertNull(state.temporaryLockedUntil());
    }

    private AuthenticationResult authenticate(String username, String passwordText) {
        char[] password = passwordText.toCharArray();
        try {
            return authService.authenticate(username, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set to run authentication tests");
        }
        return value;
    }

    private void resetAdminState() throws SQLException {
        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE users
                        SET status = 'ACTIVE',
                            lock_reason = NULL,
                            locked_at = NULL,
                            locked_by_user_id = NULL
                        WHERE username = ?
                        """)) {
                    statement.setString(1, ADMIN_USERNAME);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE user_login_security ls
                        JOIN users u ON u.id = ls.user_id
                        SET ls.consecutive_failed_attempts = 0,
                            ls.last_failed_at = NULL,
                            ls.temporary_locked_until = NULL,
                            ls.last_successful_login_at = NULL
                        WHERE u.username = ?
                        """)) {
                    statement.setString(1, ADMIN_USERNAME);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void setFailedAttempts(int attempts) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE user_login_security ls
                     JOIN users u ON u.id = ls.user_id
                     SET ls.consecutive_failed_attempts = ?,
                         ls.last_failed_at = CURRENT_TIMESTAMP(6),
                         ls.temporary_locked_until = NULL
                     WHERE u.username = ?
                     """)) {
            statement.setInt(1, attempts);
            statement.setString(2, ADMIN_USERNAME);
            statement.executeUpdate();
        }
    }

    private void setTemporaryLock(LocalDateTime lockedUntil) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE user_login_security ls
                     JOIN users u ON u.id = ls.user_id
                     SET ls.consecutive_failed_attempts = 5,
                         ls.last_failed_at = CURRENT_TIMESTAMP(6),
                         ls.temporary_locked_until = ?
                     WHERE u.username = ?
                     """)) {
            statement.setTimestamp(1, Timestamp.valueOf(lockedUntil));
            statement.setString(2, ADMIN_USERNAME);
            statement.executeUpdate();
        }
    }

    private void setAccountStatusLocked() throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE users
                     SET status = 'LOCKED',
                         lock_reason = 'S1-01 integration test',
                         locked_at = CURRENT_TIMESTAMP(6)
                     WHERE username = ?
                     """)) {
            statement.setString(1, ADMIN_USERNAME);
            statement.executeUpdate();
        }
    }

    private LoginSecurityState readSecurityState() throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT ls.consecutive_failed_attempts, ls.temporary_locked_until
                     FROM user_login_security ls
                     JOIN users u ON u.id = ls.user_id
                     WHERE u.username = ?
                     """)) {
            statement.setString(1, ADMIN_USERNAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                Timestamp lockedUntil = resultSet.getTimestamp("temporary_locked_until");
                return new LoginSecurityState(
                        resultSet.getInt("consecutive_failed_attempts"),
                        lockedUntil == null ? null : lockedUntil.toLocalDateTime()
                );
            }
        }
    }

    private record LoginSecurityState(int failedAttempts, LocalDateTime temporaryLockedUntil) {
    }
}
