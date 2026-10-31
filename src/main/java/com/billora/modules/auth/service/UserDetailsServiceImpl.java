package com.billora.modules.auth.service;

import com.billora.modules.auth.entity.User;
import com.billora.modules.auth.repository.UserRepository;
import com.billora.modules.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email != null ? email.trim() : "")
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));

        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("User account is disabled: " + email);
        }

        return new CustomUserDetails(user);
    }
}
