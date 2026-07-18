package com.zensyra.muskop.core.resource.auth;

import com.zensyra.muskop.core.dto.response.ResponseDTO;
import com.zensyra.muskop.core.service.auth.AuthService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/auth")
public class AuthResource {

    @Inject
    AuthService authService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDTO getUsername(@QueryParam("user") String username){
        return authService.findUser(username);
    }
}
