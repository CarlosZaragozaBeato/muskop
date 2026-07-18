package com.zensyra.muskop.core.repository.auth;

import com.zensyra.muskop.core.domain.auth.MuskopUser;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<MuskopUser, Integer> {

    public Optional<MuskopUser> findByUsername(String username){
        return find("username", username).firstResultOptional();
    }
}
