package com.ecetasci.hrmanagement.security;

import com.ecetasci.hrmanagement.security.CustomUserDetailsService;
import com.ecetasci.hrmanagement.security.JwtAuhenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final CustomUserDetailsService userDetailsService;
	private final JwtAuhenticationFilter jwtAuhenticationFilter;
	
	@Bean
	public PasswordEncoder getPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
	@Bean
	public AuthenticationManager getAuthenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
// enable CORS and disable CSRF since the API is stateless
				.cors().and()
				.csrf(csrf -> csrf.disable())
// configure URL based authorization
				.authorizeHttpRequests(auth -> auth
// public endpoints for authentication and user registration/verification
								.requestMatchers(
										"/api/auth/**",
										"/api/user/login",
										"/api/user/register",
										"/api/user/forgot-password",
										"/api/user/verify"
								).permitAll()
// documentation endpoints should be publicly accessible
								.requestMatchers(
										"/swagger-ui.html",
										"/swagger-ui/**",
										"/v3/api-docs/**",
										"/swagger-resources/**",
										"/webjars/**"
								).permitAll()
// publicly viewable company reviews
								.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/reviews/public").permitAll()
// site admin endpoints
								.requestMatchers(
										"/api/admin/**",
										"/api/dashboard/admin/**",
										"/api/reviews/company/**"
								).hasRole("SITE_ADMIN")
// company admin (manager) endpoints
								.requestMatchers(
										"/api/manager/**",
										"/api/company/**",
										"/api/dashboard/company/**",
										"/api/expenses/company/**",
										"/api/company/shifts/**"
								).hasRole("COMPANY_ADMIN")
// employee endpoints
								.requestMatchers(
										"/api/employee/**",
										"/api/dashboard/employee/**",
										"/api/expenses/employee/**"
								).hasRole("EMPLOYEE")
// any other request must be authenticated
								.anyRequest().authenticated()
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(jwtAuhenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

}