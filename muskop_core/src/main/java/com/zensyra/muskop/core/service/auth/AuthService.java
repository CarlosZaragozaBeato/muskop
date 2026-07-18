package com.zensyra.muskop.core.service.auth;

import com.zensyra.muskop.core.domain.auth.MuskopUser;
import com.zensyra.muskop.core.dto.auth.MuskopUserDTO;
import com.zensyra.muskop.core.dto.response.ResponseDTO;
import com.zensyra.muskop.core.exceptions.auth.NoUsernameException;
import com.zensyra.muskop.core.repository.auth.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    public ResponseDTO findUser(String username) {
        Optional<MuskopUser> optUser = userRepository.findByUsername(username);

        if (optUser.isPresent()) {
            MuskopUser user = optUser.get();
            return new ResponseDTO(new MuskopUserDTO(user.id, user.getUsername()), "User found");
        }
        throw new NoUsernameException(username);
    }
}
