package com.zensyra.muskop.core.exceptions.auth;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NoUsernameExceptionMapper implements ExceptionMapper<NoUsernameException> {

    @Override
    public Response toResponse(NoUsernameException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new com.zensyra.muskop.core.dto.response.ResponseDTO(null,
                        "User not found: " + exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
