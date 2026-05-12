package com.mvpiq.exception;

import com.mvpiq.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class);

    @Override
    public Response toResponse(Exception exception) {
        ErrorResponse errorResponse;
        
        if (exception instanceof ResourceNotFoundException) {
            errorResponse = ErrorResponse.notFound(exception.getMessage(), getCurrentPath());
            LOGGER.warn("Resource not found: " + exception.getMessage());
        } else if (exception instanceof IllegalStateException) {
            errorResponse = ErrorResponse.badRequest(exception.getMessage(), getCurrentPath());
            LOGGER.warn("Illegal state: " + exception.getMessage());
        } else if (exception instanceof jakarta.ws.rs.NotFoundException) {
            errorResponse = ErrorResponse.notFound("Resource not found", getCurrentPath());
        } else if (exception instanceof jakarta.ws.rs.BadRequestException) {
            errorResponse = ErrorResponse.badRequest("Invalid request", getCurrentPath());
        } else if (exception instanceof jakarta.ws.rs.ForbiddenException) {
            errorResponse = ErrorResponse.forbidden("Access forbidden", getCurrentPath());
        } else if (exception instanceof jakarta.ws.rs.NotAuthorizedException) {
            errorResponse = ErrorResponse.unauthorized("Unauthorized access", getCurrentPath());
        } else if (exception instanceof java.lang.IllegalArgumentException) {
            errorResponse = ErrorResponse.badRequest(exception.getMessage(), getCurrentPath());
        } else if (exception instanceof java.lang.RuntimeException) {
            errorResponse = ErrorResponse.internalServerError(exception.getMessage(), getCurrentPath());
            LOGGER.error("Runtime exception: " + exception.getMessage(), exception);
        } else {
            errorResponse = ErrorResponse.internalServerError("Internal server error", getCurrentPath());
            LOGGER.error("Unexpected exception: " + exception.getMessage(), exception);
        }
        
        return Response.status(errorResponse.getStatus())
                       .entity(errorResponse)
                       .build();
    }
    
    private String getCurrentPath() {
        // In a real implementation, you might get this from the request context
        return "/api";
    }
}
