package com.forwardmeasure.platform.dashboard;

import java.net.URI;

/** A validated same-origin absolute path exposed to the browser. */
record PlatformApplicationPath(String value) {

    static PlatformApplicationPath parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalid(raw);
        }
        if (!raw.startsWith("/")
                || raw.startsWith("//")
                || raw.indexOf('\\') >= 0
                || raw.chars().anyMatch(Character::isWhitespace)) {
            throw invalid(raw);
        }

        final URI uri;
        try {
            uri = URI.create(raw);
        } catch (IllegalArgumentException invalidUri) {
            throw new IllegalArgumentException(
                    "Platform application href must be a same-origin absolute path: "
                            + raw,
                    invalidUri);
        }
        if (uri.isAbsolute()
                || uri.getRawAuthority() != null
                || uri.getRawFragment() != null
                || uri.getRawQuery() != null
                || uri.getRawPath() == null
                || !uri.getRawPath().startsWith("/")
                || !uri.normalize().equals(uri)) {
            throw invalid(raw);
        }
        return new PlatformApplicationPath(uri.toASCIIString());
    }

    private static IllegalArgumentException invalid(String raw) {
        return new IllegalArgumentException(
                "Platform application href must be a same-origin absolute path: "
                        + raw);
    }
}
