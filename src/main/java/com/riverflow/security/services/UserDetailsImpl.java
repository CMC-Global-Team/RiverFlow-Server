package com.riverflow.security.services;

import com.riverflow.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String email;
    private String password; // Đây là passwordHash
    private Boolean isEmailVerified; // Thêm trường này
    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String email, String password, Boolean isEmailVerified,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.isEmailVerified = isEmailVerified;
        this.authorities = authorities;
    }

    /**
     * Hàm "factory" tiện lợi để chuyển đổi từ User (model) sang UserDetails (security)
     */
    public static UserDetailsImpl build(User user) {
        // Chuyển đổi Enum Role (USER, ADMIN) thành danh sách "quyền"
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().name())
        );

        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getEmailVerified(), // Truyền trạng thái xác thực
                authorities);
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password; // Spring Security sẽ dùng cái này để so sánh
    }

    @Override
    public String getUsername() {
        return email;
    }

    // Các hàm kiểm tra trạng thái tài khoản
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isEmailVerified;
    }

    // (Các hàm equals và hashCode cơ bản)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}