package com.saleswarehouse.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record AuthenticatedUser(
        long id,
        String username,
        String fullName,
        List<String> roles
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public AuthenticatedUser {
        roles = List.copyOf(roles);
    }
}
