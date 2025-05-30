package com.htc.licenseapproval.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.htc.licenseapproval.service.UserService;
import com.htc.licenseapproval.service.jpaauditing.ApplicationAuditAware;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	@Lazy
	UserService userService;
	
	@Autowired
	private AppFilter filter;

	@Bean
    PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userService);
		authenticationProvider.setPasswordEncoder(passwordEncoder());
		return authenticationProvider;
	}

	@Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	    http
	        .csrf(csrf -> csrf.disable())
	        .authorizeHttpRequests(auth -> auth
	            .requestMatchers(
	            	"/auth/registerUser",
	                "/auth/login",
	                "/auth/forgotpassword",
	                "/auth/changePassword/**",
	                "/auth/login/otpVerification" 	          
	            ).permitAll()
	            .requestMatchers("/auth/deleteUser", "/auth/logout").authenticated()
	            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
	            .anyRequest().permitAll()
	        )
	        .authenticationProvider(authenticationProvider())
			.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
	        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
	        .cors(cors -> cors.configurationSource(corsConfigurationSource()));

	    return http.build();
	}

	@Bean	
	AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
	
	@Bean
	CorsConfigurationSource corsConfigurationSource() {
	    CorsConfiguration configuration = new CorsConfiguration();
	    configuration.addAllowedOrigin("http://localhost:5173");
	    configuration.addAllowedMethod("*");  
	    configuration.addAllowedHeader("*");  
	    configuration.setAllowCredentials(true); 
	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    source.registerCorsConfiguration("/**", configuration);
	    return source;
	}

	@Bean
    AuditorAware<String> auditorAware(){
		return new ApplicationAuditAware();
	}

}