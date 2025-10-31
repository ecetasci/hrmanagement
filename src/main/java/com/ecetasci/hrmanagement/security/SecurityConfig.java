package com.ecetasci.hrmanagement.security;

import com.ecetasci.hrmanagement.constant.Endpoints;
import com.ecetasci.hrmanagement.security.JwtAuhenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
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
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
// configure URL based authorization
				.authorizeHttpRequests(auth -> auth
// publicly viewable company reviews (generic: accept both /api/reviews/public and /api/v1/dev/reviews/public)
								.requestMatchers(HttpMethod.GET,
										"/api/reviews/public",
										"/api/v1/reviews/public",
										"/api/v1/dev/reviews/public"
								).permitAll()
// public endpoints for authentication and user registration/verification
						.requestMatchers(
								Endpoints.AUTH + "/**",
								Endpoints.USER + "/login",
								Endpoints.USER + "/register",
								Endpoints.USER + "/forgot-password",
								Endpoints.USER + "/verify",
								Endpoints.USER + "/reset-password",
								Endpoints.USER + "/public/**",
								Endpoints.ADMIN + "/create-application-company",
								//Endpoints.USER + "/find-by-id/**",
								Endpoints.REVIEWS + "/public"
							).permitAll()

// documentation endpoints should be publicly accessible
						.requestMatchers(
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**",
								"/swagger-resources/**",
								"/webjars/**",
									"/error"

						).permitAll()

// site admin endpoints
						.requestMatchers(
								Endpoints.ADMIN + "/**",
								Endpoints.DASHBOARD + "/admin/**",
								Endpoints.USER + "/find-all",
								Endpoints.USER + "/find-by-id/**",
								Endpoints.USER +"user/find-by-username",
								Endpoints.USER + "/update-user-profile",
								Endpoints.USER + "/update-user-password",
								Endpoints.REVIEWS + "/admin/publish",
								Endpoints.REVIEWS + "/admin/delete"
						).hasRole("SITE_ADMIN")
// company admin (manager) endpoints
						.requestMatchers(
								//Endpoints.MANAGER + "/**",
								Endpoints.COMPANY + "/**",
								Endpoints.DASHBOARD + "/company/**",
								Endpoints.EXPENSES + "/company/**",
								Endpoints.COMPANY_SHIFTS + "/**",
								Endpoints.SHIFT + "/**",
								Endpoints.LEAVE_TYPES + "/**",
								Endpoints.DEPARTMENTS + "/**",
								Endpoints.USER + "/company-admin/update-user-password",
								Endpoints.MANAGER + "/employees/**",
								Endpoints.MANAGER +"/employee-register",
								Endpoints.ADMIN + "/list-company",
								Endpoints.REVIEWS + "/company/**",
								Endpoints.ASSETS + "/**",
								Endpoints.DASHBOARD + "/manager/**",
								Endpoints.EXPENSES + "/manager/**",
								//Endpoints.MANAGER+ "/leaves/**",
								Endpoints.MANAGER+ "/leaves/{id}/approve",
								Endpoints.MANAGER+ "/assets/**",
								Endpoints.MANAGER + "/expenses/**"



						).hasRole("COMPANY_ADMIN")
// employee endpoints
						.requestMatchers(
								Endpoints.EMPLOYEE + "/**",
								Endpoints.DASHBOARD + "/employee/**",
								Endpoints.EXPENSES + "/employee/**",
								Endpoints.SHIFT + "/**"

						).hasRole("EMPLOYEE")
// any other request must be authenticated
					//	.anyRequest().authenticated()
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(jwtAuhenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

}