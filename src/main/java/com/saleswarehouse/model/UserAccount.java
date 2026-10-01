package com.saleswarehouse.model;

import java.time.LocalDateTime;
import java.util.List;

public record UserAccount(
        long id,
        String username,
        String fullName,
        String passwordHash,
        UserStatus status,
        int consecutiveFailedAttempts,
        LocalDateTime temporaryLockedUntil,
        List<String> roles
) {
    public UserAccount {
        roles = List.copyOf(roles);
    }
}
