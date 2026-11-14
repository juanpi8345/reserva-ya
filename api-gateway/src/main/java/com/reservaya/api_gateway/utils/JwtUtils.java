package com.reservaya.api_gateway.utils;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

@Component
public class JwtUtils {
	@Value("${security.jwt.private.key}")
	private String jwtKey;
	@Value("${security.jwt.user.generator}")
	private String userGenerator;
	
	public DecodedJWT validateToken(String token) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(jwtKey);
			JWTVerifier verifier = JWT.require(algorithm)
					.withIssuer(userGenerator)
					.build();
			DecodedJWT decodeJwt = verifier.verify(token);
			return decodeJwt;
		}catch(JWTVerificationException e) {
			throw new JWTVerificationException("Invalid token, not authorized");
		}
	}
	
	public String extractUsername(DecodedJWT decodeJwt) {
		return decodeJwt.getSubject().toString();
	}
	
	public Claim getSpecificClaim(DecodedJWT decodeJwt,String claimName) {
		return decodeJwt.getClaim(claimName);
	}
	
	public Map<String,Claim> returnAllClaims(DecodedJWT decodeJwt){
		return decodeJwt.getClaims();
	}	
}
