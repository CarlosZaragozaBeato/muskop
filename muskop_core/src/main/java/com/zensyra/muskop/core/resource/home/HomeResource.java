package com.zensyra.muskop.core.resource.home;


import com.zensyra.muskop.core.service.home.HomeService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/home")
public class HomeResource {

    @Inject
    HomeService homeService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String obtenerHome(@QueryParam("user") String nombre){
        return homeService.home();
    }


}