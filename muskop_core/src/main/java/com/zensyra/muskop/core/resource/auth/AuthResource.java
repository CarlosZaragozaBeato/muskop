package com.zensyra.muskop.core.resource.auth;


import com.zensyra.muskop.core.service.auth.AuthService;
import com.zensyra.muskop.core.service.home.HomeService;
import com.zensyra.muskop.core.utils.Response;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.net.http.HttpResponse;

@Path("/api/v1/auth")
public class AuthResource{

    @Inject
    AuthService authService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getUsername(@QueryParam("user") String username){
        return  authService.findUser(username);
    }


}