package org.apache.coyote.http11.response;

public enum StatusCode {
    OK(200),
    FOUND(302),
    UNAUTHORIZED(401);

    private final int code;

    StatusCode(final int code) {
        this.code = code;
    }

    public String getStatus() {
        return String.join(" ", String.valueOf(code), name());
    }
}
