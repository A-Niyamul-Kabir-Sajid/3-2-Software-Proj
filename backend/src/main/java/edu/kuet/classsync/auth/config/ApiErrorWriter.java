package edu.kuet.classsync.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kuet.classsync.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Writes an {@link ApiError} from inside the security filter chain (before controllers run). */
@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String message)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getOutputStream(), ApiError.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(req.getRequestURI())
                .build());
    }
}
