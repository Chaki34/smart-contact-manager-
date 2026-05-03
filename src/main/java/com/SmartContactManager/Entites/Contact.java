package com.SmartContactManager.Entites;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ContactManager_Contacts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_phone",
                        columnNames = {"phoneno", "user_id"}
                )
        }
)
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String nickname;

    @NotBlank
    private String work;

    @NotBlank
    @Column(nullable = false)
    private String phoneno;

    private String image;

    @Column(length = 3000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;





    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }




    public Contact(Long id, String name, String nickname, String work, String phoneno, String image, String description) {
        this.id = id;
        this.name=name;
        this.description=description;
        this.phoneno=phoneno;
        this.image=image;
        this.nickname=nickname;
        this.work=work;
    }


    // default constructor


    public Contact() {
    }

    public String getNickname() {
        return nickname;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getPhoneno() {
        return phoneno;
    }

    public void setPhoneno(String phoneno) {
        this.phoneno = phoneno;
    }

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
