package com.reservaya.api_gateway.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.reservaya.api_gateway.security.config.filter.IpRateLimiterFilter;
import com.reservaya.api_gateway.security.config.filter.JwtTokenValidator;
import com.reservaya.api_gateway.utils.JwtUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Autowired
	private JwtUtils jwtUtils;

	@Autowired
	private IpRateLimiterFilter ipRateLimiterFilter;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
		return httpSecurity
				.csrf(csrf -> csrf.disable())
			    .httpBasic(basic -> basic.disable())
			    .formLogin(form -> form.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
					    .requestMatchers("/api/auth", "/api/auth/**", "/error", "/fallback/**").permitAll()
					    .requestMatchers(HttpMethod.GET, "/api/v1/bookings").hasRole("ADMIN")
					    .requestMatchers(HttpMethod.GET, "/api/v1/payments").hasRole("ADMIN")
					    .anyRequest().authenticated()
				)
				.addFilterBefore(ipRateLimiterFilter, BasicAuthenticationFilter.class)
				.addFilterAfter(new JwtTokenValidator(jwtUtils), IpRateLimiterFilter.class)
				.build();						
	}
}
