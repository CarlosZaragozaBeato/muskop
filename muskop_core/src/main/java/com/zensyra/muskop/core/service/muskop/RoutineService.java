package com.zensyra.muskop.core.service.muskop;

import com.zensyra.muskop.core.domain.auth.MuskopUser;
import com.zensyra.muskop.core.domain.muskop.Routine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class RoutineService {

    @Transactional
    public void saveNewRoutine(Integer userId, String routineName) {

        MuskopUser user = MuskopUser.findById(userId);

        if (user == null) {
            throw new NotFoundException("The user with ID " + userId + " doesn't exist");
        }

        Routine newRoutine = new Routine();
        newRoutine.name = routineName;
        newRoutine.user = user;
        user.rutinas.add(newRoutine);

        newRoutine.persist();
    }
}
