package com.mvpiq.exception;

import com.mvpiq.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        ErrorResponse errorResponse;
        
        if (exception instanceof jakarta.ws.rs.NotFoundException) {
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
        } else {
            errorResponse = ErrorResponse.internalServerError("Internal server error", getCurrentPath());
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
