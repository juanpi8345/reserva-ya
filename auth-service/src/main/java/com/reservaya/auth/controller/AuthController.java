package com.reservaya.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reservaya.auth.model.AuthLoginRequest;
import com.reservaya.auth.model.AuthLoginResponse;
import com.reservaya.auth.model.AuthRegisterRequest;
import com.reservaya.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
	
	private final AuthService authService;

	@PostMapping
	public ResponseEntity<AuthLoginResponse> authenticate(@Valid @RequestBody AuthLoginRequest request){
		return ResponseEntity.ok(authService.login(request));
	}
	
	@PostMapping("/register")
	public ResponseEntity<Void> register(@Valid @RequestBody AuthRegisterRequest request){
		authService.register(request);
		return ResponseEntity.ok().build();
	}
}
