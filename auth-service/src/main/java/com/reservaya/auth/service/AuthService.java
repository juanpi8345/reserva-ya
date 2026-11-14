package com.reservaya.auth.service;

import com.reservaya.auth.model.AuthLoginRequest;
import com.reservaya.auth.model.AuthLoginResponse;
import com.reservaya.auth.model.AuthRegisterRequest;

import jakarta.validation.Valid;

public interface AuthService {
	AuthLoginResponse login(@Valid AuthLoginRequest userRequest);
	void register(@Valid AuthRegisterRequest userRequest);
}
