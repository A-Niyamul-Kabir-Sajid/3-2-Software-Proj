package edu.kuet.classsync.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Uniform error body returned by every failing endpoint. Created with the Builder pattern:
 * {@code ApiError.builder().status(404).message("...").path("/api/v1/x").build()}.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldErrorItem> fieldErrors) {

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Instant timestamp = Instant.now();
        private int status;
        private String error;
        private String message;
        private String path;
        private final List<FieldErrorItem> fieldErrors = new ArrayList<>();

        private Builder() { }

        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public Builder status(int status) { this.status = status; return this; }

        public Builder error(String error) { this.error = error; return this; }

        public Builder message(String message) { this.message = message; return this; }

        public Builder path(String path) { this.path = path; return this; }

        public Builder fieldError(String field, String message) {
            this.fieldErrors.add(new FieldErrorItem(field, message));
            return this;
        }

        public ApiError build() {
            return new ApiError(timestamp, status, error, message, path, List.copyOf(fieldErrors));
        }
    }
}
