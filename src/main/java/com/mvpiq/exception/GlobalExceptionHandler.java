package com.mvpiq.exception;

import com.mvpiq.dto.ErrorResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(Exception exception) {
        ErrorResponse errorResponse;

        String path = uriInfo != null ? uriInfo.getPath() : "/api";
        String contentType = headers != null ? headers.getHeaderString("Content-Type") : "unknown";

        if (exception instanceof ResourceNotFoundException) {
            errorResponse = ErrorResponse.notFound(exception.getMessage(), path);
            LOGGER.warn("Resource not found: " + exception.getMessage() + " at " + path);
        } else if (exception instanceof IllegalStateException) {
            errorResponse = ErrorResponse.badRequest(exception.getMessage(), path);
            LOGGER.warn("Illegal state: " + exception.getMessage() + " at " + path);
        } else if (exception instanceof jakarta.ws.rs.NotFoundException) {
            errorResponse = ErrorResponse.notFound("Resource not found", path);
        } else if (exception instanceof jakarta.ws.rs.BadRequestException) {
            errorResponse = ErrorResponse.badRequest(exception.getMessage(), path);
        } else if (exception instanceof jakarta.ws.rs.ForbiddenException) {
            errorResponse = ErrorResponse.forbidden("Access forbidden", path);
        } else if (exception instanceof jakarta.ws.rs.NotAuthorizedException) {
            errorResponse = ErrorResponse.unauthorized("Unauthorized access", path);
        } else if (exception instanceof java.lang.IllegalArgumentException) {
            errorResponse = ErrorResponse.badRequest(exception.getMessage(), path);
        } else if (exception instanceof java.lang.RuntimeException) {
            errorResponse = ErrorResponse.internalServerError(exception.getMessage(), path);
            LOGGER.error("Runtime exception: " + exception.getMessage() + " at " + path + " with Content-Type: " + contentType, exception);
        } else {
            errorResponse = ErrorResponse.internalServerError("Internal server error", path);
            LOGGER.error("Unexpected exception: " + exception.getMessage() + " at " + path + " with Content-Type: " + contentType, exception);
        }

        return Response.status(errorResponse.getStatus())
                       .entity(errorResponse)
                       .build();
    }
}
