package com.zensyra.muskop.core.domain.auth;

import com.zensyra.muskop.core.domain.muskop.Resource;
import com.zensyra.muskop.core.domain.muskop.Routine;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="users")
public class MuskopUser extends PanacheEntityBase {

    @Id
    public String username;

    // Dentro de com.tuempresa.app.domain.Usuario

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Routine> rutinas = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Resource> recursos = new ArrayList<>();

    public MuskopUser() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
