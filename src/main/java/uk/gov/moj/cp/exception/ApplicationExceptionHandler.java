package uk.gov.moj.cp.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
@RestControllerAdvice
public class ApplicationExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    public ResponseEntity<String> handleHttpStatusCodeExceptionNotFound(HttpStatusCodeException e, HttpServletRequest request) {

        log.error("Case not found for request [{}]: status={}",
                  request.getRequestURI(), e.getStatusCode());

        return ResponseEntity
            .status(e.getStatusCode())
            .body(e.getResponseBodyAsString());
    }

    @ExceptionHandler(HttpClientErrorException.TooManyRequests.class)
    public ResponseEntity<String> handleHttpStatusCodeExceptionTooManyRequests(HttpStatusCodeException e, HttpServletRequest request) {

        log.error("Downstream service error (Too Many Requests) for request [{}]: status={}, body={}",
                  request.getRequestURI(), e.getStatusCode(), e.getResponseBodyAsString(), e);

        return ResponseEntity
            .status(e.getStatusCode())
            .body(e.getResponseBodyAsString());
    }

    @ExceptionHandler(HttpServerErrorException.ServiceUnavailable.class)
    public ResponseEntity<String> handleHttpStatusCodeExceptionServiceUnavailable(HttpStatusCodeException e, HttpServletRequest request) {

        log.error("Downstream service error (Service Unavailable) for request [{}]: status={}, body={}",
                  request.getRequestURI(), e.getStatusCode(), e.getResponseBodyAsString(), e);

        return ResponseEntity
            .status(e.getStatusCode())
            .body(e.getResponseBodyAsString());
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<String> handleHttpStatusCodeException(HttpStatusCodeException e, HttpServletRequest request) {

        log.error("Downstream service error for request [{}]: status={}, body={}",
                  request.getRequestURI(), e.getStatusCode(), e.getResponseBodyAsString(), e);

        return ResponseEntity
            .status(e.getStatusCode())
            .body(e.getResponseBodyAsString());
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<String> handleHttpServerErrorException(HttpStatusCodeException e, HttpServletRequest request) {

        log.error("Downstream service error for request [{}]: status={}, body={}",
                  request.getRequestURI(), e.getStatusCode(), e.getResponseBodyAsString(), e);

        return ResponseEntity
            .status(e.getStatusCode())
            .body(e.getResponseBodyAsString());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {

        log.error("Invalid request for [{}]: {}", request.getRequestURI(), e.getMessage(), e);

        return ResponseEntity
            .badRequest()
            .body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error for request [{}]", request.getRequestURI(), e);

        return ResponseEntity
            .internalServerError()
            .body("An error occurred while processing, see the logs for more details");
    }
}

