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
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/v1/api/dev/user/login",
								"/v1/api/dev/user/register"
						).permitAll()
						.requestMatchers(
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**",
								"/swagger-resources/**",
								"/webjars/**"
						).permitAll()
						.requestMatchers("/v1/api/dev/admin/user/get-all-users").hasRole("SITE_ADMIN")
						.requestMatchers("api/user/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("api/expenses/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("api/employee/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("api/admin/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

						.requestMatchers("/public/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("api/dashboard/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("api/company/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("api/manager/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().authenticated()
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(jwtAuhenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

}