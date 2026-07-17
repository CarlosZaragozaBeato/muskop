package com.zensyra.muskop.core.service.muskop;

import com.zensyra.muskop.core.domain.auth.MuskopUser;
import com.zensyra.muskop.core.domain.muskop.Routine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RoutineService {

    @Transactional
    public void saveNewRoutine(Long userId, String routineName) {

        MuskopUser user = MuskopUser.findById(userId);

        Routine newRoutine = new Routine();
        newRoutine.name = routineName;

        newRoutine.user = user;
        user.rutinas.add(newRoutine);

        user.persist();
    }
}
