package com.acme.model;

import jakarta.validation.constraints.NotBlank;
import lombok.With;
import org.hibernate.validator.constraints.Length;

public record User(
        @With Long id,
        @NotBlank @Length(min = 6) String username,
        @NotBlank @Length(min = 12) String password,
        @NotBlank String name
) {

    public static final String REDACTED_PASSWORD = "[password redacted]";

    public User withRedactedPassword() {
        return new User(id, username, REDACTED_PASSWORD, name);
    }

    public static User newWithRedactedPassword(Long id, String username, String name) {
        return new User(id, username, REDACTED_PASSWORD, name);
    }
}
