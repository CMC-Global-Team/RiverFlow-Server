package com.riverflow.config.jwt; // (Hoặc package security của bạn)

import com.riverflow.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final boolean isActive;
    private final boolean isLocked;
    private final User.Role role;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String password, boolean isActive, boolean isLocked, User.Role role,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.isActive = isActive;
        this.isLocked = isLocked;
        this.role = role;
        this.authorities = authorities;
    }

    /**
     * Phương thức tĩnh (static) để tạo UserPrincipal từ User entity
     */
    public static UserPrincipal create(User user) {
        String roleName;
        switch (user.getRole()) {
            case super_admin:
                roleName = "ROLE_SUPER_ADMIN";
                break;
            case admin:
                roleName = "ROLE_ADMIN";
                break;
            default:
                roleName = "ROLE_USER";
        }
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getStatus() == User.UserStatus.active,
                user.getStatus() == User.UserStatus.suspended,
                user.getRole(),
                Collections.singleton(new SimpleGrantedAuthority(roleName)));
    }

    public Long getId() {
        return id;
    }

    public User.Role getRole() {
        return role;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
