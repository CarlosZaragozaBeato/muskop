package com.zensyra.muskop.core.service.muskop;

import com.zensyra.muskop.core.domain.auth.MuskopUser;
import com.zensyra.muskop.core.domain.muskop.Resource;
import com.zensyra.muskop.core.dto.http.CreateResourceRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ResourceService {

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public Integer createResource(CreateResourceRequestDTO request){
        MuskopUser user = MuskopUser.findById(request.userId());

        if (user == null){
            throw new IllegalArgumentException("The user with ID " + request.userId() + "doesn't exists");
        }

        try{
            String jsonContent = objectMapper.writeValueAsString(request.tab());

            Resource resource = new Resource();
            resource.title = request.title();
            resource.type = request.type();
            resource.content = jsonContent;
            resource.user = user;

            resource.persist();
            return resource.id;
        }catch (Exception e){
            throw new RuntimeException("Error processing tablature JSON", e);
        }
    }

}
