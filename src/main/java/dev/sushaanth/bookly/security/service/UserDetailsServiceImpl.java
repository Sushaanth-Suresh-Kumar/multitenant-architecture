package dev.sushaanth.bookly.security.service;

import dev.sushaanth.bookly.multitenancy.context.TenantContext;
import dev.sushaanth.bookly.security.model.LibraryUser;
import dev.sushaanth.bookly.security.repository.LibraryUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final LibraryUserRepository userRepository;

    public UserDetailsServiceImpl(LibraryUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Store current tenant context
        String currentTenant = TenantContext.getTenantId();

        try {
            // Clear tenant context to ensure we query from public schema
            TenantContext.clear();

            // Find the user
            LibraryUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

            return new User(
                    user.getUsername(),
                    user.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getAuthority()))
            );
        } finally {
            // Restore previous tenant context
            if (currentTenant != null) {
                TenantContext.setTenantId(currentTenant);
            }
        }
    }
}