package com.zensyra.muskop.core.resource.muskop;

import com.zensyra.muskop.core.dto.http.CreateResourceRequestDTO;
import com.zensyra.muskop.core.service.muskop.ResourceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.awt.*;

@Path("/api/v1/resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ResourceResource {

    @Inject
    ResourceService resourceService;

    @POST
    public Response create(CreateResourceRequestDTO request){
        Integer idCreated = resourceService.createResource(request);
        return Response.status(Response.Status.CREATED).entity(idCreated).build();
    }
}
