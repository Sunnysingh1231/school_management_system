package com.sms.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.sms.custSuccessHandeler.CustSuccessHandeler;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
	
	@Autowired
	private final CustSuccessHandeler custSuccessHandeler;

	public SecurityConfiguration(CustSuccessHandeler custSuccessHandeler) {
		this.custSuccessHandeler = custSuccessHandeler;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity sec) throws Exception{
		
		sec
			.authorizeHttpRequests(auth->auth
				.requestMatchers("/").permitAll()
				.requestMatchers("/index.css","/header.css","/navbar.css").permitAll()
				.requestMatchers("/super/**").hasAnyRole("SUPER_ADMIN")
				.requestMatchers("/admin/**").hasAnyRole("SCHOOL_ADMIN")
				.requestMatchers("/teacher/**").hasAnyRole("TEACHER")
				.requestMatchers("/student/**").hasAnyRole("STUDENT")
				.anyRequest().authenticated()
			)
			.formLogin(form -> form
					.loginPage("/login")
					
//					.defaultSuccessUrl("/",true)
					.successHandler(custSuccessHandeler)
					.permitAll()
				)
			.logout(logout->logout
					.logoutUrl("/logout")
					.logoutSuccessUrl("/")
					.permitAll()
				); 
		
		return sec.build();
	}
	
	@Bean
	public PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder(4);
	}
}
