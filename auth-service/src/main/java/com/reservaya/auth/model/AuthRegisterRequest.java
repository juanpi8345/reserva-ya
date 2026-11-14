package com.reservaya.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest (@NotBlank @Size(min = 3, max = 30) String username,
							  	   @NotBlank @Size(min = 8, max = 72) String password,
							  	   @NotBlank @Email String email,
							  	   @NotBlank @Pattern(regexp = "^\\+?[0-9]{8,15}$") String phone){}
