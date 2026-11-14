package com.reservaya.api_gateway.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.reservaya.api_gateway.utils.JwtUtils;

import static org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions.circuitBreaker;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Slf4j
@Configuration
public class RouteConfig {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final JwtUtils jwtUtils;
    private final String GLOBAL_RATE_LIMITER = "globalRateLimiter";
    private final String GLOBAL_TIME_LIMITER = "globalTimeLimiter";

    public RouteConfig(RateLimiterRegistry rateLimiterRegistry, JwtUtils jwtUtils) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.jwtUtils = jwtUtils;
    }
    
    @Bean
    public RouterFunction<ServerResponse> authServiceRoute() {
        return route("auth-service")
                .route(path("/api/auth/**"), http())
                .filter(rateLimiterFilter())
                .filter(circuitBreaker(GLOBAL_TIME_LIMITER))
                .filter(lb("AUTH-SERVICE"))
                .filter(stripPrefix(1))
                .build();
    }
	
    @Bean
    public RouterFunction<ServerResponse> bookingServiceRoute() {
        return route("booking-service")
                .route(path("/api/v1/bookings/**"), http())
                .filter(rateLimiterFilter())
                .filter(circuitBreaker(GLOBAL_TIME_LIMITER))
                .filter(addUserIdHeaderFilter())
                .filter(lb("BOOKING-SERVICE"))
                .filter(stripPrefix(1))
                .build();
    }
    
    @Bean
    public RouterFunction<ServerResponse> paymentServiceRoute() {
        return route("payment-service")
                .route(path("/api/v1/payments/**"), http())
                .filter(rateLimiterFilter())
                .filter(circuitBreaker(GLOBAL_TIME_LIMITER))
                .filter(lb("PAYMENT-SERVICE"))
                .filter(stripPrefix(1))
                .build();
    }
    
    @Bean
    public RouterFunction<ServerResponse> notificationServiceRoute() {
        return route("notification-service")
                .route(path("/api/v1/notifications/**"), http())
                .filter(rateLimiterFilter())
                .filter(circuitBreaker(GLOBAL_TIME_LIMITER))
                .filter(addUserIdHeaderFilter())
                .filter(lb("NOTIFICATION-SERVICE"))
                .filter(stripPrefix(1))
                .build();
    }


    private HandlerFilterFunction<ServerResponse, ServerResponse> addUserIdHeaderFilter() {
        return (request, next) -> {
            String authHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }

            String token = authHeader.substring(7);
            try {
                DecodedJWT jwt = jwtUtils.validateToken(token);
                Long userId = jwtUtils.getSpecificClaim(jwt, "userId").asLong();
                if (userId == null) {
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
                }

                ServerRequest modifiedRequest = ServerRequest.from(request)
                        .headers(headers -> {
                            headers.remove("X-User-Id");
                            headers.add("X-User-Id", String.valueOf(userId));
                        })
                        .build();
                return next.handle(modifiedRequest);

            } catch (Exception e) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }
        };
    }
    
    private HandlerFilterFunction<ServerResponse, ServerResponse> rateLimiterFilter() {
        return (request, next) -> {
            String clientIp = getClientIp(request);
            RateLimiter ipRateLimiter = rateLimiterRegistry.rateLimiter("ip-" + clientIp, GLOBAL_RATE_LIMITER);

            if (ipRateLimiter.acquirePermission()) {
                return next.handle(request);
            } else {
                return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }
        };
    }

    private String getClientIp(ServerRequest request) {
        String xForwardedFor = request.headers().firstHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.remoteAddress()
                .map(addr -> addr.getAddress().getHostAddress())
                .orElse("unknown-ip");
    }
}
