package com.mvpiq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private Long timestamp;

    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

    public static ErrorResponse badRequest(String message, String path) {
        return of(400, "Bad Request", message, path);
    }

    public static ErrorResponse notFound(String message, String path) {
        return of(404, "Not Found", message, path);
    }

    public static ErrorResponse internalServerError(String message, String path) {
        return of(500, "Internal Server Error", message, path);
    }

    public static ErrorResponse unauthorized(String message, String path) {
        return of(401, "Unauthorized", message, path);
    }

    public static ErrorResponse forbidden(String message, String path) {
        return of(403, "Forbidden", message, path);
    }
}
