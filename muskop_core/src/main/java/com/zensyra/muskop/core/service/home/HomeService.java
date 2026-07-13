package com.zensyra.muskop.core.service.home;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HomeService {

    public String home(){
        return "HOME";
    }

}
