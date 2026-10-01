package com.saleswarehouse.dao;

import com.saleswarehouse.model.UserAccount;
import com.saleswarehouse.model.UserStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private static final String FIND_USER_FOR_UPDATE_SQL = """
            SELECT id, username, full_name, password_hash, status
            FROM users
            WHERE username = ?
            FOR UPDATE
            """;

    private static final String FIND_LOGIN_SECURITY_SQL = """
            SELECT consecutive_failed_attempts, temporary_locked_until
            FROM user_login_security
            WHERE user_id = ?
            FOR UPDATE
            """;

    private static final String FIND_ROLES_SQL = """
            SELECT r.code
            FROM user_roles ur
            JOIN roles r ON r.id = ur.role_id
            WHERE ur.user_id = ? AND r.is_active = TRUE
            ORDER BY r.code
            """;

    public Optional<UserAccount> findByUsernameForUpdate(Connection connection, String username)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_USER_FOR_UPDATE_SQL)) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                long userId = resultSet.getLong("id");
                ensureLoginSecurity(connection, userId);
                LoginSecurityState securityState = findLoginSecurityForUpdate(connection, userId);

                return Optional.of(new UserAccount(
                        userId,
                        resultSet.getString("username"),
                        resultSet.getString("full_name"),
                        resultSet.getString("password_hash"),
                        UserStatus.valueOf(resultSet.getString("status")),
                        securityState.failedAttempts(),
                        securityState.temporaryLockedUntil(),
                        findRoles(connection, userId)
                ));
            }
        }
    }

    public void recordFailedLogin(
            Connection connection,
            long userId,
            int failedAttempts,
            LocalDateTime failedAt,
            LocalDateTime temporaryLockedUntil
    ) throws SQLException {
        String sql = """
                UPDATE user_login_security
                SET consecutive_failed_attempts = ?,
                    last_failed_at = ?,
                    temporary_locked_until = ?
                WHERE user_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, failedAttempts);
            statement.setTimestamp(2, Timestamp.valueOf(failedAt));
            if (temporaryLockedUntil == null) {
                statement.setNull(3, java.sql.Types.TIMESTAMP);
            } else {
                statement.setTimestamp(3, Timestamp.valueOf(temporaryLockedUntil));
            }
            statement.setLong(4, userId);
            statement.executeUpdate();
        }
    }

    public void resetFailedLogins(Connection connection, long userId, LocalDateTime successfulAt)
            throws SQLException {
        String sql = """
                UPDATE user_login_security
                SET consecutive_failed_attempts = 0,
                    last_failed_at = NULL,
                    temporary_locked_until = NULL,
                    last_successful_login_at = ?
                WHERE user_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(successfulAt));
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    public void clearExpiredTemporaryLock(Connection connection, long userId) throws SQLException {
        String sql = """
                UPDATE user_login_security
                SET consecutive_failed_attempts = 0,
                    last_failed_at = NULL,
                    temporary_locked_until = NULL
                WHERE user_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    private void ensureLoginSecurity(Connection connection, long userId) throws SQLException {
        String sql = """
                INSERT INTO user_login_security (user_id)
                VALUES (?)
                ON DUPLICATE KEY UPDATE user_id = user_id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    private LoginSecurityState findLoginSecurityForUpdate(Connection connection, long userId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_LOGIN_SECURITY_SQL)) {
            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Missing login security row for user id " + userId);
                }

                Timestamp lockedUntil = resultSet.getTimestamp("temporary_locked_until");
                return new LoginSecurityState(
                        resultSet.getInt("consecutive_failed_attempts"),
                        lockedUntil == null ? null : lockedUntil.toLocalDateTime()
                );
            }
        }
    }

    private List<String> findRoles(Connection connection, long userId) throws SQLException {
        List<String> roles = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(FIND_ROLES_SQL)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    roles.add(resultSet.getString("code"));
                }
            }
        }
        return roles;
    }

    private record LoginSecurityState(int failedAttempts, LocalDateTime temporaryLockedUntil) {
    }
}
