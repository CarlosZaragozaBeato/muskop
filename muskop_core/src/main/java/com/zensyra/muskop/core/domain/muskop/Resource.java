package com.zensyra.muskop.core.domain.muskop;


import com.zensyra.muskop.core.domain.auth.MuskopUser;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name="resource")
public class Resource extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    public String title;
    public String type;
    public String category;

    @Column(columnDefinition = "TEXT")
    public String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public MuskopUser user;

    public Resource() {
    }
}
