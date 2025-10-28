package com.ecetasci.hrmanagement.security;

import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findUserByEmail(email)
		                          .orElseThrow(() -> new UsernameNotFoundException("User bulunamadı: " + email));
		return new UserPrincipal(user);
	}
}