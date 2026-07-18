package com.zensyra.muskop.core.resource.muskop;

import com.zensyra.muskop.core.dto.muskop.ResourceDetailDTO;
import com.zensyra.muskop.core.dto.muskop.ResourceSummaryDTO;
import com.zensyra.muskop.core.dto.response.CreateResourceRequestDTO;
import com.zensyra.muskop.core.dto.response.UpdateResourceRequestDTO;
import com.zensyra.muskop.core.service.muskop.ResourceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

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

    @GET
    public List<ResourceSummaryDTO> list(@QueryParam("userId") Integer userId,
                                         @QueryParam("type") String type,
                                         @QueryParam("category") String category) {
        return resourceService.listByUser(userId, type, category);
    }

    @GET
    @Path("/{id}")
    public ResourceDetailDTO get(@PathParam("id") Integer id) {
        return resourceService.getById(id);
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, UpdateResourceRequestDTO request) {
        resourceService.updateResource(id, request);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        resourceService.deleteResource(id);
        return Response.noContent().build();
    }
}
