package com.reservaya.auth.service.impl;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.reservaya.auth.entity.OutboxEvent;
import com.reservaya.auth.entity.Role;
import com.reservaya.auth.entity.UserSec;
import com.reservaya.auth.event.model.UserCreatedEvent;
import com.reservaya.auth.exception.UsernameAlreadyExistsException;
import com.reservaya.auth.model.AuthLoginRequest;
import com.reservaya.auth.model.AuthLoginResponse;
import com.reservaya.auth.model.AuthRegisterRequest;
import com.reservaya.auth.model.EventType;
import com.reservaya.auth.model.OutboxStatus;
import com.reservaya.auth.repository.OutboxEventRepository;
import com.reservaya.auth.repository.RoleRepository;
import com.reservaya.auth.repository.UserRepository;
import com.reservaya.auth.security.CustomUserDetails;
import com.reservaya.auth.service.AuthService;
import com.reservaya.auth.utils.JwtUtils;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
	
	private final UserDetailsService userDetailsService;
	private final JwtUtils jwtUtils;
	private final PasswordEncoder passwordEncoder;
	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;
	private final OutboxEventRepository outboxEventRepository;
	
	@Override
	public AuthLoginResponse login(@Valid AuthLoginRequest userRequest) {
		String username = userRequest.username();
		String password = userRequest.password();
		
		Authentication authentication = this.authenticate(username, password);
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		String accessToken = jwtUtils.createToken(authentication, userDetails.getId());
		return new AuthLoginResponse(username, "Login successful", accessToken);
	}
	
	public Authentication authenticate(String username, String password) {
		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		if (userDetails == null || !passwordEncoder.matches(password, userDetails.getPassword())) {
			throw new BadCredentialsException("Invalid username or password");
		}
		return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
	}
	
	@Transactional
	@Override 
	public void register(@Valid AuthRegisterRequest userRequest) {
		if(userRepository.existsByUsername(userRequest.username()))
		    throw new UsernameAlreadyExistsException(userRequest.username());
		
		Role role = roleRepository.findByRole("USER").orElseThrow(); 
		
		UserSec userSec = new UserSec();
		userSec.setUsername(userRequest.username());
		userSec.setRole(role);
		userSec.setPassword(passwordEncoder.encode(userRequest.password()));
		UserSec savedUser = userRepository.save(userSec);
		
		UserCreatedEvent userCreatedEvent =
				new UserCreatedEvent(savedUser.getId(), userRequest.phone(), userRequest.email());
		
		OutboxEvent event = OutboxEvent.builder().eventType(EventType.USER_CREATED)
				.partitionKey(userSec.getId()).status(OutboxStatus.PENDING)
				.payload(objectMapper.writeValueAsString(userCreatedEvent))
				.build();
		
		outboxEventRepository.save(event);
	}
}
