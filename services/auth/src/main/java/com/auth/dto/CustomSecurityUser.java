package com.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
public class CustomSecurityUser extends User {
    @Getter
    private long id;

    public CustomSecurityUser(String username, String password, long id,String...authorities) {
        super(username, password, AuthorityUtils.createAuthorityList(authorities));
        this.id = id;
    }
}
