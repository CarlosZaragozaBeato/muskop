package com.zensyra.muskop.core.service.auth;

import com.zensyra.muskop.core.domain.auth.MuskopUser;
import com.zensyra.muskop.core.dto.auth.MuskopUserDTO;
import com.zensyra.muskop.core.exceptions.auth.NoUsernameException;
import com.zensyra.muskop.core.repository.auth.UserRepository;
import com.zensyra.muskop.core.utils.Response;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class AuthService {

    @Inject
    private UserRepository userRepository;

    public Response findUser(String username) {
        Optional<MuskopUser> optUser = userRepository.findByUsername(username);

        if (optUser.isPresent()) {
            return new Response(new MuskopUserDTO(optUser.get().getUsername()), HttpResponseStatus.FOUND.toString());
        }
        throw new NoUsernameException(username);
    }
}
