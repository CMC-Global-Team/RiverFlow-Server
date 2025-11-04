package com.riverflow.security.services;

import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Spring Security sẽ gọi hàm này.
     * "username" ở đây chính là email mà người dùng nhập vào.
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Dùng UserRepository để tìm user trong CSDL
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        // Chuyển đổi User (model) sang UserDetails (security)
        return UserDetailsImpl.build(user);
    }

    /**
     * Đây là hàm helper mà SignInService của bạn đang dùng
     * để lấy đầy đủ Entity User (thay vì chỉ UserDetails)
     */
    @Transactional
    public User loadUserEntityByEmail(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));
    }
}