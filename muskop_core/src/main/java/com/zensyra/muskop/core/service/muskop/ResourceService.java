package com.zensyra.muskop.core.service.muskop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zensyra.muskop.core.domain.auth.MuskopUser;
import com.zensyra.muskop.core.domain.muskop.Resource;
import com.zensyra.muskop.core.dto.muskop.ResourceDetailDTO;
import com.zensyra.muskop.core.dto.muskop.ResourceSummaryDTO;
import com.zensyra.muskop.core.dto.response.CreateResourceRequestDTO;
import com.zensyra.muskop.core.dto.response.UpdateResourceRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class ResourceService {

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public Integer createResource(CreateResourceRequestDTO request){
        MuskopUser user = MuskopUser.findById(request.userId());

        if (user == null){
            throw new NotFoundException("The user with ID " + request.userId() + " doesn't exist");
        }

        Resource resource = new Resource();
        resource.title = request.title();
        resource.type = request.type();
        resource.category = request.category();
        resource.content = writeContent(request.content());
        resource.user = user;

        resource.persist();
        return resource.id;
    }

    public List<ResourceSummaryDTO> listByUser(Integer userId, String type, String category) {
        List<Resource> resources = Resource.list("user.id", userId);
        return resources.stream()
                .filter(r -> type == null || type.equalsIgnoreCase(r.type))
                .filter(r -> category == null || category.equalsIgnoreCase(r.category))
                .map(r -> new ResourceSummaryDTO(r.id, r.title, r.type, r.category))
                .toList();
    }

    public ResourceDetailDTO getById(Integer id) {
        Resource resource = findOrFail(id);
        return new ResourceDetailDTO(resource.id, resource.title, resource.type,
                resource.category, readContent(resource.content));
    }

    @Transactional
    public void updateResource(Integer id, UpdateResourceRequestDTO request) {
        Resource resource = findOrFail(id);
        resource.title = request.title();
        resource.type = request.type();
        resource.category = request.category();
        resource.content = writeContent(request.content());
    }

    @Transactional
    public void deleteResource(Integer id) {
        Resource resource = findOrFail(id);
        resource.delete();
    }

    private Resource findOrFail(Integer id) {
        Resource resource = Resource.findById(id);
        if (resource == null) {
            throw new NotFoundException("The resource with ID " + id + " doesn't exist");
        }
        return resource;
    }

    private String writeContent(JsonNode content) {
        if (content == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing resource content JSON", e);
        }
    }

    private JsonNode readContent(String content) {
        if (content == null) {
            return null;
        }
        try {
            return objectMapper.readTree(content);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing stored resource content", e);
        }
    }
}
