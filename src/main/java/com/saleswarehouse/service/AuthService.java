package com.saleswarehouse.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.saleswarehouse.dao.UserDAO;
import com.saleswarehouse.model.AuthenticatedUser;
import com.saleswarehouse.model.UserAccount;
import com.saleswarehouse.model.UserStatus;
import com.saleswarehouse.util.DatabaseConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int TEMPORARY_LOCK_MINUTES = 15;

    // Used only to keep invalid-username checks close to the BCrypt cost of valid users.
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$12$80DX0n9VVnCPjtjUWgQV5OqdzSa0MlCcmSYlXj9sJxq.SzP5JlHAC";

    private final DatabaseConnectionFactory connectionFactory;
    private final UserDAO userDAO;
    private final Clock clock;

    public AuthService() {
        this(new DatabaseConnectionFactory(), new UserDAO(), Clock.systemDefaultZone());
    }

    AuthService(DatabaseConnectionFactory connectionFactory, UserDAO userDAO, Clock clock) {
        this.connectionFactory = connectionFactory;
        this.userDAO = userDAO;
        this.clock = clock;
    }

    public AuthenticationResult authenticate(String rawUsername, char[] password) {
        String username = rawUsername == null ? "" : rawUsername.trim();

        if (username.isBlank() || password == null || password.length == 0) {
            verifyPassword(password == null ? new char[0] : password, DUMMY_BCRYPT_HASH);
            return AuthenticationResult.failure(AuthenticationFailure.INVALID_CREDENTIALS);
        }

        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Optional<UserAccount> optionalUser = userDAO.findByUsernameForUpdate(connection, username);
                if (optionalUser.isEmpty()) {
                    verifyPassword(password, DUMMY_BCRYPT_HASH);
                    connection.rollback();
                    return AuthenticationResult.failure(AuthenticationFailure.INVALID_CREDENTIALS);
                }

                UserAccount user = optionalUser.get();
                LocalDateTime now = LocalDateTime.now(clock);

                if (user.status() == UserStatus.LOCKED) {
                    connection.rollback();
                    return AuthenticationResult.failure(AuthenticationFailure.ACCOUNT_LOCKED);
                }
                if (user.status() == UserStatus.INACTIVE) {
                    connection.rollback();
                    return AuthenticationResult.failure(AuthenticationFailure.ACCOUNT_INACTIVE);
                }

                int failedAttempts = user.consecutiveFailedAttempts();
                if (user.temporaryLockedUntil() != null) {
                    if (now.isBefore(user.temporaryLockedUntil())) {
                        connection.rollback();
                        return AuthenticationResult.failure(AuthenticationFailure.TEMPORARILY_LOCKED);
                    }

                    userDAO.clearExpiredTemporaryLock(connection, user.id());
                    failedAttempts = 0;
                }

                if (!verifyPassword(password, user.passwordHash())) {
                    int updatedAttempts = Math.min(failedAttempts + 1, MAX_FAILED_ATTEMPTS);
                    LocalDateTime lockedUntil = updatedAttempts >= MAX_FAILED_ATTEMPTS
                            ? now.plusMinutes(TEMPORARY_LOCK_MINUTES)
                            : null;

                    userDAO.recordFailedLogin(
                            connection,
                            user.id(),
                            updatedAttempts,
                            now,
                            lockedUntil
                    );
                    connection.commit();
                    return AuthenticationResult.failure(AuthenticationFailure.INVALID_CREDENTIALS);
                }

                userDAO.resetFailedLogins(connection, user.id(), now);
                connection.commit();

                return AuthenticationResult.success(new AuthenticatedUser(
                        user.id(),
                        user.username(),
                        user.fullName(),
                        user.roles()
                ));
            } catch (SQLException | RuntimeException exception) {
                rollbackQuietly(connection);
                throw exception;
            }
        } catch (SQLException | IllegalStateException exception) {
            throw new AuthenticationException("Unable to authenticate user", exception);
        }
    }

    private boolean verifyPassword(char[] password, String passwordHash) {
        try {
            return BCrypt.verifyer().verify(password, passwordHash.toCharArray()).verified;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // The original exception remains the relevant failure.
        }
    }
}
