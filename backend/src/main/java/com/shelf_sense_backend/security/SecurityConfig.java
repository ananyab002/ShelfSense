package com.shelf_sense_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.shelf_sense_backend.service.UserService;



@Configuration
public class SecurityConfig {

	private final JwtFilter jwtFilter;
	private final UserService userService;
	private final PasswordEncoder passwordEncoder;

	public SecurityConfig(JwtFilter jwtFilter, UserService userService, PasswordEncoder passwordEncoder) {
		this.jwtFilter = jwtFilter;
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/auth/**").permitAll()
						.requestMatchers("/users/search").authenticated()
						.requestMatchers(HttpMethod.POST, "/contacts").authenticated()
						.requestMatchers(HttpMethod.GET, "/contacts").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/receipts/extract-text").authenticated()
						.anyRequest().authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowCredentials(true);
		configuration.addAllowedOriginPattern("http://localhost:3001");
		configuration.addAllowedOriginPattern("http://localhost:5173");
		configuration.addAllowedOriginPattern("http://backend:8080");
		configuration.addAllowedHeader("*");
		configuration.addAllowedMethod("*");
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = http
				.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder
				.userDetailsService(userService)
				.passwordEncoder(passwordEncoder);
		return authenticationManagerBuilder.build();
	}

}