package com.reservaya.auth.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.reservaya.auth.entity.Role;
import com.reservaya.auth.entity.UserSec;
import com.reservaya.auth.repository.UserRepository;
import com.reservaya.auth.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	
	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserSec userSec = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
		Role role = userSec.getRole();
		
		List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
		authorityList.add(new SimpleGrantedAuthority("ROLE_".concat(role.getRole())));
	
		userSec.getRole().getPermissionSet()
			.stream()
			.forEach(permission -> authorityList.add(new SimpleGrantedAuthority(permission.getPermissionName())));
		
		return new CustomUserDetails(
				userSec.getId(),
				userSec.getUsername(),
				userSec.getPassword(),
				userSec.isEnabled(),
				userSec.isAccountNonExpired(),
				userSec.isCredentialNonExpired(),
				userSec.isAccountNonLocked(),
				authorityList);
	}
}
