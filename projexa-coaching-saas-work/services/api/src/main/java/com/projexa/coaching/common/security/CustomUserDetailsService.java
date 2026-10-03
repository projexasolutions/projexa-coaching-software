package com.projexa.coaching.common.security;
import com.projexa.coaching.users.repository.UserRepository; import org.springframework.security.core.userdetails.*; import org.springframework.stereotype.Service;
@Service public class CustomUserDetailsService implements UserDetailsService { private final UserRepository repo; public CustomUserDetailsService(UserRepository repo){this.repo=repo;} public UserDetails loadUserByUsername(String username){throw new UsernameNotFoundException("Use tenant-aware authentication");} }
