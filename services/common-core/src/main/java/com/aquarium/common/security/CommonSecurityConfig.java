package com.aquarium.common.security;

import com.aquarium.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.Locale;

/**
 * Cấu hình bảo mật dùng chung cho mọi microservice (trừ gateway):
 * <ul>
 *   <li>Stateless, xác thực bằng JWT Bearer — không session, không form/basic login.</li>
 *   <li>Deny-by-default: chỉ các endpoint khai báo trong {@code aquarium.security.public-endpoints} được truy cập ẩn danh.</li>
 *   <li>CORS được quản lý tập trung ở API Gateway nên tắt ở service (tránh header CORS bị trùng).</li>
 *   <li>Header bảo mật: nosniff, X-Frame-Options DENY, Referrer-Policy, CSP cho /api/**.</li>
 *   <li>Giới hạn kích thước body request.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, AquariumSecurityProperties.class})
@SecurityScheme(name = "BearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
public class CommonSecurityConfig {

    @Bean
    public JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    @Bean
    public FilterRegistrationBean<RequestSizeLimitFilter> requestSizeLimitFilter(AquariumSecurityProperties properties) {
        FilterRegistrationBean<RequestSizeLimitFilter> registration =
                new FilterRegistrationBean<>(new RequestSizeLimitFilter(properties.getMaxRequestBytes()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /** Vô hiệu hóa user mặc định của Spring Boot (không dùng đăng nhập form/basic). */
    @Bean
    public UserDetailsService noopUserDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Form/basic login is disabled");
        };
    }

    @Bean
    public SecurityFilterChain aquariumSecurityFilterChain(HttpSecurity http,
                                                          JwtService jwtService,
                                                          AquariumSecurityProperties properties) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                new AntPathRequestMatcher("/api/**"),
                                new StaticHeadersWriter("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/error").permitAll();
                    auth.requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    for (String entry : properties.getPublicEndpoints()) {
                        String trimmed = entry.trim();
                        if (trimmed.isEmpty()) {
                            continue;
                        }
                        int space = trimmed.indexOf(' ');
                        if (space > 0) {
                            HttpMethod method = HttpMethod.valueOf(trimmed.substring(0, space).toUpperCase(Locale.ROOT));
                            auth.requestMatchers(method, trimmed.substring(space + 1).trim()).permitAll();
                        } else {
                            auth.requestMatchers(trimmed).permitAll();
                        }
                    }
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            boolean invalidToken = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE) != null;
            ErrorCode code = invalidToken ? ErrorCode.INVALID_TOKEN : ErrorCode.UNAUTHENTICATED;
            response.setHeader("WWW-Authenticate", invalidToken ? "Bearer error=\"invalid_token\"" : "Bearer");
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, code);
        };
    }

    private static AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.UNAUTHORIZED);
    }

    private static void writeJson(HttpServletResponse response, int status, ErrorCode code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"code\":" + code.getCode()
                + ",\"message\":\"" + code.getMessage().replace("\"", "\\\"") + "\"}");
    }
}
