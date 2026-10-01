package com.saleswarehouse.service;

import com.saleswarehouse.model.AuthenticatedUser;

public record AuthenticationResult(
        boolean successful,
        AuthenticatedUser user,
        AuthenticationFailure failure
) {
    public static AuthenticationResult success(AuthenticatedUser user) {
        return new AuthenticationResult(true, user, null);
    }

    public static AuthenticationResult failure(AuthenticationFailure failure) {
        return new AuthenticationResult(false, null, failure);
    }
}
